(ns clj-amqp.test.core
  (:use clojure.test
        clj-amqp.core))

(deftest with-channel-test
  (with-channel "me"
    (is (= *channel* "me"))))
