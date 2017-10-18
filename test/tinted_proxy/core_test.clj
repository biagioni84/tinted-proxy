(ns tinted-proxy.core-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [tinted-proxy.core :refer :all]
            [org.httpkit.client :as http]))
(def dest-req (atom nil))
(def t
  (let [dest-resp-headers {:content-type "application/json"
                           :h1           "v1"}]
    (with-redefs [http/request (fn [req]
                                 (reset! dest-req req)
                                 {:status  200
                                  :headers dest-resp-headers
                                  :body    "resp body"})]
      (let [h-not-proxy (constantly {:status 403})
            h (wrap-tinted-proxy h-not-proxy
                                 "/google" "http://www.google.com"
                                 :update-req-body-fn #(str (:body %) "al final del request")
                                 :update-req-headers-fn #(:headers (assoc-in % [:headers "token"] "t"))
                                 :update-resp-body-fn #(str (:body %) "al final del response")
                                 :update-resp-headers-fn #(dissoc (:headers %) :content-type)
                                 )
            {resp-headers1 :headers resp-body1 :body} (h {:request-method :post
                                                          :headers        {}
                                                          :uri            "/google/test"
                                                          :body           "req body"})
            {resp-status2 :status} (h {:request-method :post
                                       :headers        {}
                                       :uri            "/google2/test"
                                       :body           "req body"})]
        (println "----------------")
        (println resp-headers1)
        (testing "We route correctly"
          (is (= (:url @dest-req) "http://www.google.com/test"))
          (is (= resp-status2 403)))
        (testing "We modify request stuff"
          (is (= (:body @dest-req) "req bodyal final del request"))
          (is (= (get-in @dest-req [:headers "token"]) "t")))
        (testing "We modify response stuff"
          (is (= resp-headers1 (dissoc dest-resp-headers :content-type)))
          (is (= resp-body1 "resp bodyal final del response")))
        ))
    )
  )