(ns clj-amqp-dsl.test.internal.connection-pool
  (:use clj-amqp-dsl.internal.connection-pool
        clj-amqp.common
        clj-rabbitmq.core
        clj-amqp-dsl.core
        clojure.test))

(defn connection-create-func []
  (connect {:host "localhost"}))

;(start connection-create-func)

(def *test-pool*)

(deftest create-pool-test
  (with-pooled-channel *test-pool*
    (println "Execute with channel Umm k")
    (is (= (num-active *test-pool*) 1)))
  (is (= (num-active *test-pool*) 0)))
