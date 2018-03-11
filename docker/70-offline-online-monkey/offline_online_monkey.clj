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

(defn get-random-node [nodes]
  (rand-nth nodes))

(defn get-replica-diff [node since]
  (try
    (-> (http/get (str "http://" node "/updates") {:query-params {:since since}
                                                   :as           :transit+json})
        :body)
    (catch Throwable e
      (timbre/error e "unable to receive replica diff from node" node)
      (lww-element-set/make-replica))))

(defn worker [i nodes normal-monkey-in-process-count]
  (let [node (get-random-node nodes)]
    (loop [local-replica (let [replica (get-replica-diff node 0)
                               _       (timbre/info "received elements" (lww-element-set/members replica)
                                                    "from" node)]
                           replica)
           last-download (lww-element-set/get-last-update local-replica)
           last-upload   (lww-element-set/get-last-update local-replica)]
      (do
        (timbre/info "last-download" last-download)
        (timbre/info "last-upload" last-upload)
        (let [sleep-ms       (rand-int 10000)
              add?           (pos? (rand-int 2))
              elements-count (inc (rand-int 10))
              elements       (repeatedly elements-count #(rand-int 100))
              [op replica]   (if add?
                               ["adding" (lww-element-set/add-elements local-replica elements)]
                               ["removing" (lww-element-set/del-elements local-replica elements)])
              sync?          (= 0 (rand-int 10))
              _              (timbre/info "worker" i ":" op "elements" (vec elements) "in local replica")
              url            (str "http://" node "/updates")]
          (Thread/sleep sleep-ms)
          (if sync?
            (do
              (timbre/info "worker" i ":" "merging replica with node" node)
              (let [remote-diff   (get-replica-diff node last-download)
                    _             (timbre/info "remote elements since" last-download
                                               ":" (lww-element-set/members remote-diff))
                    last-download (max last-download (lww-element-set/get-last-update remote-diff))
                    local-diff    (lww-element-set/filter-replica (fn [[_ timestamp]]
                                                                    (< last-upload timestamp))
                                                                  local-replica)
                    last-upload   (try
                                    (timbre/info "uploading elements" (lww-element-set/members local-diff))
                                    (http/post (str "http://" node "/update")
                                               {:form-params  local-diff
                                                :content-type :transit+json})
                                    (max last-upload (lww-element-set/get-last-update local-diff))
                                    (catch Throwable e
                                      (timbre/error e "unable to replicate diff to node" node)
                                      last-upload))
                    replica       (lww-element-set/merge-replicas local-replica remote-diff)
                    _             (timbre/info "merged elements" (lww-element-set/members local-diff))]
                (timbre/info "last-download" last-download)
                (timbre/info "last-upload" last-upload)
                (recur replica
                       last-download
                       last-upload)))
            (recur replica last-download last-upload)))))))

(let [[_
       normal-monkey-in-process-count
       & nodes]                      (vec clojure.core/*command-line-args*)
      normal-monkey-in-process-count (Integer/parseInt normal-monkey-in-process-count)
      _                              (timbre/info "running with nodes" (vec nodes))
      _                              (timbre/info "running" normal-monkey-in-process-count "threads")
      threads                        (for [i (range normal-monkey-in-process-count)]
                                       (doto (Thread. #(worker i nodes normal-monkey-in-process-count))
                                         (.start)))]
  (doseq [t threads]
    (.join t)))
