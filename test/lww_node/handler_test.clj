(ns lww-node.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [lww-node.handler :refer :all]
            [clj-http.client :as http]
            [taoensso.timbre :as timbre]
            [lww-element-set.core :as lww-element-set]))

(deftest test-app
  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(deftest members-handler-test
  (testing "members handler should return a set with elements"
    (let [{:keys [body]} (http/get "http://localhost:3002/" {:as :transit+json})]
      (is (set? body))
      (is (every? string? body)))))

(deftest insert-handler-test
  (testing "inserting element"
    (let [e (str (java.util.UUID/randomUUID))]
      (println "element" e)
      (http/post "http://localhost:3001/insert" {:form-params  [e]
                                                 :content-type :transit+json})
      (let [{:keys [body]} (http/get "http://localhost:3001/" {:as :transit+json})]
        (is (get body e)))))
  (testing "inserting elements"
    (let [e1 (str (java.util.UUID/randomUUID))
          e2 (str (java.util.UUID/randomUUID))]
      (println "element" e1)
      (println "element" e2)
      (http/post "http://localhost:3001/insert" {:form-params  [e1 e2]
                                                 :content-type :transit+json})
      (let [{:keys [body]} (http/get "http://localhost:3001/" {:as :transit+json})]
        (is (get body e1))
        (is (get body e2))))))

(deftest delete-handler-test
  (testing "inserting and removing element"
    (let [e (str (java.util.UUID/randomUUID))]
      (println "element" e)
      (http/post "http://localhost:3001/insert" {:form-params  [e]
                                                 :content-type :transit+json})
      (http/post "http://localhost:3001/delete" {:form-params  [e]
                                                 :content-type :transit+json})
      (let [{:keys [body]} (http/get "http://localhost:3001/" {:as :transit+json})]
        (is (not (get body e))))))
  (testing "inserting, removing and inserting element"
    (let [e (str (java.util.UUID/randomUUID))]
      (println "element" e)
      (http/post "http://localhost:3001/insert" {:form-params  [e]
                                                 :content-type :transit+json})
      (http/post "http://localhost:3001/delete" {:form-params  [e]
                                                 :content-type :transit+json})
      (http/post "http://localhost:3001/insert" {:form-params  [e]
                                                 :content-type :transit+json})
      (let [{:keys [body]} (http/get "http://localhost:3001/" {:as :transit+json})]
        (is (get body e)))))
  (testing "inserting to 3001 node, delete from 3002 node, wait for replication"
    (let [e (str (java.util.UUID/randomUUID))]
      (println "element" e)
      (http/post "http://localhost:3001/insert" {:form-params  [e]
                                                 :content-type :transit+json})
      (Thread/sleep 1000)
      (http/post "http://localhost:3002/delete" {:form-params  [e]
                                                 :content-type :transit+json})
      (Thread/sleep 1000)
      (let [{:keys [body]} (http/get "http://localhost:3001/" {:as :transit+json})]
        (is (not (get body e))))))
  (testing "inserting to 3001 node, delete from 3002 node, add to 3003 node, wait for replication"
    (let [e (str (java.util.UUID/randomUUID))]
      (http/post "http://localhost:3001/insert" {:form-params  [e]
                                                 :content-type :transit+json})
      (Thread/sleep 1000)
      (http/post "http://localhost:3002/delete" {:form-params  [e]
                                                 :content-type :transit+json})
      (Thread/sleep 1000)
      (http/post "http://localhost:3003/insert" {:form-params  [e]
                                                 :content-type :transit+json})
      (Thread/sleep 1000)
      (let [{:keys [body]} (http/get "http://localhost:3001/" {:as :transit+json})]
        (is (get body e))))))

(deftest updates-handler-test
  (testing "receive all updates, insert, receive updates again"
    (let [{:keys [body]} (http/get "http://localhost:3001/updates" {:as           :transit+json
                                                                    :query-params {:since 0}})]
      (is (map? body))
      (let [last-update (lww-element-set/get-last-update body)
            e           (str (java.util.UUID/randomUUID))]
        (http/post "http://localhost:3001/insert" {:form-params  [e]
                                                   :content-type :transit+json})
        (let [{:keys [body]}  (http/get "http://localhost:3001/updates" {:as           :transit+json
                                                                         :query-params {:since last-update}})
              new-last-update (lww-element-set/get-last-update body)]
          (is (= #{e} (lww-element-set/members body)))
          (is (not= new-last-update last-update))
          (let [{:keys [body]}  (http/get "http://localhost:3001/updates" {:as           :transit+json
                                                                           :query-params {:since new-last-update}})]
            (is (lww-element-set/empty-replica? body))))))))

(comment

  (http/post "http://localhost:3001/delete" {:form-params  [0 62]
                                             :content-type :transit+json})
  (http/post "http://localhost:3001/insert" {:form-params  [2]
                                             :content-type :transit+json})
  (http/post "http://localhost:3001/insert" {:form-params  [4]
                                             :content-type :transit+json})
  (http/post "http://localhost:3001/delete" {:form-params  [200 199]
                                             :content-type :transit+json})
  )
