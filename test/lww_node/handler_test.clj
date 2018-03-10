(ns lww-node.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [lww-node.handler :refer :all]
            [clj-http.client :as http]))

(deftest test-app
  (testing "main route"
    (let [response (app (mock/request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) "Hello World"))))

  (testing "not-found route"
    (let [response (app (mock/request :get "/invalid"))]
      (is (= (:status response) 404)))))

(comment
  (http/post "http://localhost:12309/insert" {:form-params  [200]
                                              :content-type :transit+json})
  (http/post "http://localhost:12309/delete" {:form-params  [2 3]
                                              :content-type :transit+json})
  (http/post "http://localhost:12309/insert" {:form-params  [2]
                                              :content-type :transit+json})
  (http/post "http://localhost:12309/insert" {:form-params  [4]
                                              :content-type :transit+json})
  (http/post "http://localhost:12309/delete" {:form-params  [200 199]
                                              :content-type :transit+json})
  )
