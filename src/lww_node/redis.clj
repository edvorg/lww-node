(ns lww-node.redis
  (:require [mount.core :as mount]
            [taoensso.carmine :as car :refer (wcar)]
            [lww-element-set.core :as lww-element-set]
            [immutant.scheduling :refer [every schedule stop id in]]
            [taoensso.timbre :as timbre]
            [rocks.clj.configuron.core :refer [env]]))

(defn server-conn
  "Get redis spec from config or use default spec."
  []
  {:pool {} :spec (or (:redis-spec env)
                      {:host "localhost" :port 6379})})

(defmacro wcar* [& body] `(car/wcar (server-conn) ~@body))

(defn get-offload-interval
  "Get offload interval from config or use default value."
  []
  (or (:offload-interval-seconds env)
      10))

(defn transact-data
  "Atomically update data in redis."
  [f]
  (last
    (wcar*
      (car/watch :data)
      (let [old-data (wcar* (car/get :data))
            new-data (f old-data)]
        (car/multi)
        (car/set :data new-data)
        (car/exec)
        (car/return new-data)))))

(defn offload
  "Offload local data diff to redis."
  [{:keys [replica last-update]}]
  (let [{:keys [add-set
                del-set]
         :as   replica-diff} (->> replica
                                  (lww-element-set/filter-replica (fn [[_ timestamp]]
                                                                    (< last-update timestamp))))
        last-update          (max last-update (lww-element-set/get-last-update replica-diff))
        replica              (if (lww-element-set/empty-replica? replica-diff)
                               replica
                               (transact-data (fn [old-replica]
                                                (lww-element-set/merge-replicas
                                                  old-replica
                                                  replica-diff))))]
    (timbre/info "offloading replica,"
                 "new add entries:" (count add-set)
                 "new del entries:" (count del-set)
                 "last-update" last-update)
    {:replica     replica
     :last-update last-update}))

(mount/defstate offloader
  :start (let [replica (wcar* (car/get :data))
               data    (atom {:last-update (lww-element-set/get-last-update replica)
                              :replica     replica})]
           {:job  (schedule
                    (fn []
                      (swap! data offload))
                    (-> (id :offloader)
                        (in (get-offload-interval) :seconds)
                        (every (get-offload-interval) :seconds)))
            :data data})
  :stop (let [{:keys [data job]} @offloader]
          (swap! data offload)
          (stop job)))
