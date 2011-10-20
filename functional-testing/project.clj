(defproject functional-testing "0.1.0-SNAPSHOT"
  :description "Contains test to verify the functionality behaves as excepted and benchmarking."
  :dependencies [[clj-amqp-dsl "0.1.0-SNAPSHOT"]
                 [clj-rabbitmq "0.1.0-SNAPSHOT"]
                 [clj-bson "0.1.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.3.1"]]
  :warn-on-reflection true)
