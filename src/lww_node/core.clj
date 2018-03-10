(ns lww-node.core
  (:gen-class)
  (:require [lww-node.server :refer [server]]
            [mount.core :as mount]
            [rocks.clj.configuron.core :refer [env]]
            [taoensso.timbre :as timbre]))

(defn start
  "Development helper to start server instance"
  []
  (mount/start (mount/with-args (cond-> {}
                                  (:port env)  (assoc :port (:port env))
                                  (:nodes env) (assoc :nodes (:nodes env))))))

(defn stop
  "Development helper to stop server instance"
  []
  (mount/stop))

(defn reset
  "Development helper to restart server instance"
  []
  (stop)
  (start))

;; dependency injection system tweak
(mount/in-cljc-mode)

(defn set-shutdown-hook [f]
  (let [shutdown-thread (new Thread f)]
    (.. Runtime (getRuntime) (addShutdownHook shutdown-thread))))

(defn -main
  "Prod build entry point"
  [& [port & nodes]]
  ;; stop DI system on jvm shutdown
  (set-shutdown-hook stop)
  ;; start DI system
  (let [state (try
                (mount/with-args (cond-> {}
                                   ;; config values
                                   (:port env)  (assoc :port (:port env))
                                   (:nodes env) (assoc :nodes (:nodes env))
                                   ;; cli arg overrides
                                   port         (assoc :port (Integer/parseInt port))
                                   nodes        (assoc :nodes nodes)))
                (catch Throwable _
                  (println "Usage:")
                  (println " Run on default port")
                  (println "    java -jar lww-node.jar")
                  (println " Run on specified port, e.g. 3000")
                  (println "    java -jar lww-node.jar 3000")
                  (println " Run on specified port and connect to cluster nodes (first node must be this node)")
                  (println "    java -jar lww-node.jar 3000 localhost:3000 localhost:3001 localhost:3002")
                  (System/exit 0)))]
    (mount/start state))
  (timbre/info "server status" @server))
