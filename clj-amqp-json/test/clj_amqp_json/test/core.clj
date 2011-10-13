(ns clj-amqp-json.test.core
  (:use [clj-amqp-json.core] :reload)
  (:use [clojure.test]))

(deftest encode-body-test
  (encode-body "hello world")
  (encode-body {:jack "hello"}))

(deftest decode-encode-body-test
  (is (= "Hello World" (decode-body (encode-body "Hello World"))))
  (is (= {:a "b" :c "d"} (decode-body (encode-body {:a "b" :c "d"})))))

(deftest create-consumer-test
  (let [consumer (create-consumer
                  (is (= body "hello world"))
                  (is (= envelope "envelope"))
                  (is (= properties {:content-type "utf8"})))
        consumer2 (create-consumer
                  (is (= body "hello world"))
                  (is (= envelope "envelope"))
                  (is (= properties "props")))]
    (consumer (encode-body "hello world")
              "envelope"
              {:content-type "utf8"})
    (consumer2 (encode-body "hello world")
               "envelope"
               "props")))
