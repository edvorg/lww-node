":"; exec /usr/bin/env lein update-in :plugins conj "[lein-exec \"0.3.6\"]" -- exec "$0" "$@"

(require '[leiningen.exec :as exec])

(exec/deps '[[clj-http "3.7.0"]
             [cheshire "5.5.0"]
             [com.taoensso/timbre "4.10.0"]
             [com.cognitect/transit-clj "0.8.300"]]
           :repositories {"central" "https://repo1.maven.org/maven2"
                          "clojars" "https://clojars.org/repo"})

(require '[clj-http.client :as http])
(require '[cheshire.core :as cheshire])
(require '[taoensso.timbre :as timbre])

(defn worker [nodes-count normal-monkey-in-process-count]
  (loop []
    (let [sleep-ms       (rand-int 10000)
          add?           (pos? (rand-int 2))
          elements-count (inc (rand-int 10))
          elements       (repeatedly elements-count #(rand-int 100))
          node-id        (rand-int nodes-count)
          node           (str "lww-set-node-" (inc node-id) ":" (+ 3001 node-id))
          [op url]       (if add?
                           ["adding" (str "http://" node "/insert")]
                           ["removing" (str "http://" node "/delete")])]
      (timbre/info op "elements" (vec elements) "in node" node)
      (try
        (http/post url {:form-params  elements
                        :content-type :transit+json})
        (catch Throwable e
          (timbre/error e "error caught")))
      (Thread/sleep sleep-ms))
    (recur)))

(let [[_
       nodes-count
       normal-monkey-in-process-count] (vec clojure.core/*command-line-args*)
      nodes-count                      (Integer/parseInt nodes-count)
      normal-monkey-in-process-count   (Integer/parseInt normal-monkey-in-process-count)
      _                                (timbre/info "running" normal-monkey-in-process-count "threads")
      threads                          (for [_ (range normal-monkey-in-process-count)]
                                         (doto (Thread. #(worker nodes-count normal-monkey-in-process-count))
                                           (.start)))]
  (doseq [t threads]
    (.join t)))
