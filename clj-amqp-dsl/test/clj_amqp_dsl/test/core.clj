(ns clj-amqp-dsl.test.core
  (:use [clj-amqp-dsl.core] :reload
        clj-rabbitmq.core
        clj-amqp-dsl.internal.channel-threads
        clj-amqp.common
        clj-bson.core)
  (:use [clojure.test])
  (:import [java.util.concurrent ConcurrentHashMap]
           [java.util Date])
  (:require [clj-amqp.channel :as channel]
            [clj-amqp-dsl.connection :as connection]))

(extend-type java.lang.String
  Closable
  (close [this]))

(defn connection-create-func []
  (connect {:host "localhost"}))

(defn consumer-handler-test-func [msg envelope properites]
  (println "Thread went to sleep")
  (println "Executed....."))

(deftest create-consumer-test
  (let [body {:a "body"}]
    (doseq [x (range 0 10)]
      ((create-consumer (fn [^bytes body props]
                          (decode body))
                        consumer-handler-test-func) (encode body) "b" "c"))))
