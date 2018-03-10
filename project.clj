(defproject lww-node "0.1.0-SNAPSHOT"
  :description "Distributed last-write-wins set in Clojure with REST API"
  :url "https://github.com/edvorg/lww-node"
  :min-lein-version "2.5.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [compojure "1.6.0"]
                 [ring/ring-defaults "0.3.1"]
                 [ring-transit "0.1.6"]
                 [rocks.clj/configuron "0.1.1-SNAPSHOT"]
                 [com.taoensso/timbre "4.10.0"]
                 [mount "0.1.12"]
                 [org.immutant/immutant "2.1.10"]
                 [lww-element-set "0.1.0-SNAPSHOT"]
                 [com.taoensso/carmine "2.17.0"]
                 [clj-http "3.8.0"]]
  :uberjar-name "lww-node.jar"
  :plugins [[lein-ring "0.12.3"]
            [lein-environ "1.1.0"]]
  :ring {:handler lww-node.handler/app}
  :main lww-node.core
  :profiles {:dev      {:repl-options {:init-ns lww-node.core}
                        :dependencies [[javax.servlet/servlet-api "2.5"]
                                       [ring/ring-mock "0.3.2"]]
                        :env          {:mode                     :dev
                                       :host                     "localhost"
                                       :port                     12309
                                       :offload-interval-seconds 30
                                       :redis-spec               {:host "localhost"
                                                                  :port 6379}
                                       :io-threads               2
                                       :worker-threads           8
                                       :nodes                    ["localhost:12309"]}}
             :uberjar  {:env {:mode                     :uberjar
                              :host                     "localhost"
                              :port                     12309
                              :offload-interval-seconds 30
                              :redis-spec               {:host "localhost"
                                                         :port 6379}
                              :io-threads               2
                              :worker-threads           8
                              :nodes                    ["localhost:12309"]}
                        :aot :all}
             :jvm-opts ["-Xms256M"
                        "-Xmx1024M"]})
