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

;(start connection-create-func)

(defn consumer-handler-test-func [msg envelope properites]
  (println "Thread went to sleep")
  (println "Executed....."))

(deftest create-consumer-test
  (let [body {:a "body"}]
    (doseq [x (range 0 100)]
      ((create-consumer (fn [^bytes body props]
                          (decode body))
                        consumer-handler-test-func) (encode body) "b" "c"))))

(deftest remove-old-channels-test
  (let [old-channels (new ConcurrentHashMap)
        new-channels (new ConcurrentHashMap)
        correct (new ConcurrentHashMap)]
    (doseq [i (range 0 10)]
      (.put old-channels i (assoc (make-channel-info i "channel")
                             :timestamp (doto (new Date)
                                          (.setTime 0))))
      (.put new-channels i (make-channel-info i "channel")))
    (do
      (remove-old-channels old-channels)
      (is (= correct old-channels))
      (remove-old-channels new-channels)
      (is (= 10 (.size new-channels))))))
