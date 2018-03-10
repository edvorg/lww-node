(ns lww-node.handler
  (:require [clj-http.client :as http]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [lww-element-set.core :as lww-element-set]
            [lww-node.redis :as redis]
            [mount.core :as mount]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.transit :refer [wrap-transit-body wrap-transit-response]]
            [ring.util.response :as response]
            [taoensso.timbre :as timbre]))

(mount/defstate last-replication-time
  :start (-> @redis/offloader
             :data
             deref
             :replica
             lww-element-set/get-last-update
             atom))

(defn- replicate-diff
  "Upload replica diff to other instances."
  [replica-diff]
  (let [last-replication-time @last-replication-time
        last-diff-update      (lww-element-set/get-last-update replica-diff)]
    (loop [old-val @last-replication-time]
      (if-not (compare-and-set! last-replication-time old-val (max last-diff-update old-val))
        (recur @last-replication-time)
        (let [replica-diff (->> replica-diff
                                (lww-element-set/filter-replica (fn [[_ timestamp]]
                                                                  (< old-val timestamp))))
              nodes        (set (rest (:nodes (mount/args))))]
          (timbre/info "replicated diff to nodes" nodes)
          (doseq [node nodes]
            (try
              (http/post (str "http://" node "/update")
                         {:form-params  replica-diff
                          :content-type :transit+json})
              (catch Throwable e
                (timbre/error e "unable to replicate diff to node" node)))))))))

(defn insert-handler
  "Add elements to current replica."
  [{elements :body}]
  (timbre/debug "inserting" (vec elements))
  (let [{:keys [replica]} (swap! (:data @redis/offloader)
                                 update :replica
                                 lww-element-set/add-elements elements)]
    (replicate-diff replica))
  (response/response "ok"))

(defn delete-handler
  "Remove elements from current replica. "
  [{elements :body}]
  (timbre/debug "deleting" (vec elements))
  (let [{:keys [replica]} (swap! (:data @redis/offloader)
                                 update :replica
                                 lww-element-set/del-elements elements)]
    (replicate-diff replica))
  (response/response "ok"))

(defn update-handler
  "Apply diff replica on current replica."
  [{replica-diff :body}]
  (timbre/debug "accepting replication diff" replica-diff)
  (swap! (:data @redis/offloader)
         update :replica
         lww-element-set/merge-replicas replica-diff))

(defn members-handler
  "Return all members in current replica."
  [_]
  (let [members (lww-element-set/members (:replica (deref (:data @redis/offloader))))]
    (timbre/debug "getting members" (vec members))
    (response/response members)))

(defroutes app-routes
  (POST "/insert" [] insert-handler)
  (POST "/delete" [] delete-handler)
  (POST "/update" [] update-handler)
  (GET "/" [] members-handler)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-transit-response {:encoding :json :opts {}})
      (wrap-transit-body {:keywords? true :opts {}})
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))
