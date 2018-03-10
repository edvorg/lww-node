":"; exec /usr/bin/env lein update-in :plugins conj "[lein-exec \"0.3.6\"]" -- exec "$0" "$@"

(require '[leiningen.exec :as exec])

(exec/deps '[[clj-http "3.7.0"]
             [cheshire "5.5.0"]
             [com.taoensso/timbre "4.10.0"]
             [com.cognitect/transit-clj "0.8.300"]
             [lww-element-set "0.1.0-SNAPSHOT"]]
           :repositories {"central" "https://repo1.maven.org/maven2"
                          "clojars" "https://clojars.org/repo"})

(require '[clj-http.client :as http])
(require '[cheshire.core :as cheshire])
(require '[taoensso.timbre :as timbre])
(require '[lww-element-set.core :as lww-element-set])

(defn get-random-node [nodes-count]
  (let [node-id (rand-int nodes-count)]
    (str "lww-set-node-" (inc node-id) ":" (+ 3001 node-id))))

(defn get-replica-diff [node since]
  (try
    (-> (http/get (str "http://" node "/updates") {:query-params {:since since}
                                                   :as           :transit+json})
        :body)
    (catch Throwable e
      (timbre/error e "unable to receive replica diff from node" node)
      (lww-element-set/make-replica))))

(defn worker [i nodes-count normal-monkey-in-process-count]
  (let [node (get-random-node nodes-count)]
    (loop [local-replica (let [replica (get-replica-diff node 0)
                               _       (timbre/info "worker" i
                                                    "received elements" (lww-element-set/members replica)
                                                    "from" node)]
                           replica)
           last-download (lww-element-set/get-last-update local-replica)]
      (let [sleep-ms      1000
            remote-diff   (get-replica-diff node last-download)
            last-download (max last-download (lww-element-set/get-last-update remote-diff))
            new-replica   (lww-element-set/merge-replicas local-replica remote-diff)]
        (when-not (lww-element-set/empty-replica? remote-diff)
          (timbre/info "worker" i ": received diff" remote-diff)
          (timbre/info "worker" i ": merged elements" (vec (lww-element-set/members new-replica))))
        (Thread/sleep sleep-ms)
        (recur new-replica last-download)))))

(let [[_
       nodes-count
       normal-monkey-in-process-count] (vec clojure.core/*command-line-args*)
      nodes-count                      (Integer/parseInt nodes-count)
      normal-monkey-in-process-count   (Integer/parseInt normal-monkey-in-process-count)
      _                                (timbre/info "running" normal-monkey-in-process-count "threads")
      threads                          (for [i (range normal-monkey-in-process-count)]
                                         (doto (Thread. #(worker i nodes-count normal-monkey-in-process-count))
                                           (.start)))]
  (doseq [t threads]
    (.join t)))
