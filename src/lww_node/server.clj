(ns lww-node.server
  (:require [immutant.web :refer [run stop]]
            [immutant.web.undertow :refer [options]]
            [rocks.clj.configuron.core :refer [env]]
            [lww-node.handler :as handler]
            [mount.core :as mount]
            [taoensso.timbre :as timbre]))

(mount/defstate ^{:on-reload :noop} server
  :start (let [{port  :port
                nodes :nodes
                :or   {port 12309}}   (mount/args)
               {:keys [mode
                       host
                       ssl-port
                       keystore
                       key-password
                       io-threads
                       worker-threads]
                :or   {host       "0.0.0.0"
                       io-threads 2}} env
               params                 (cond-> {:host host}
                                        io-threads     (assoc :io-threads io-threads)
                                        worker-threads (assoc :worker-threads worker-threads)
                                        ssl-port       (assoc :ssl-port (str ssl-port)
                                                              :keystore keystore
                                                              :key-password key-password
                                                              :client-auth :need)
                                        port           (assoc :port (str port)))
               handler                (case (:mode env)
                                        :uberjar #'handler/app ;; TODO optimized routes for prod
                                        :dev     #'handler/app)]
           (timbre/info "starting server in" mode "mode with params" params "nodes" (distinct nodes))
           {:server (run handler (options params))
            :nodes  nodes})
  :stop (let [{:keys [server]} @server]
          (timbre/info "stopping server")
          (stop server)))
