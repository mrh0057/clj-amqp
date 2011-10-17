(ns clj-amqp-dsl.test.internal.channel-threads
  (:use clojure.test
        clj-amqp-dsl.internal.channel-threads))

(deftest get-channel-test
  (remove-all-channels)
  (let [thread-id 1
        channel "me"]))
