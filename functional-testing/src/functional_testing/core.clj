(ns functional-testing.core
  (:use clj-rabbitmq.core
        clj-amqp.core
        clj-amqp-dsl.core
        clj-bson.core)
  (:require [clj-amqp-dsl.internal.long-term-connection :as long-term]
            [clj-amqp.common :as common]))

(defn- connection-function [username password]
  (fn []
    (connect {:host "localhost"
              :username username
              :password password})))

(defn setup-connection [username password pool-size]
  (start (connection-function username password) pool-size))

(defn direct-publish-test-consumer []
  (thread-channel
   (fn []
     (consume "test-queue-direct"
              (create-consumer (fn [^bytes b-body props]
                                     (decode b-body))
                                   (fn [msg envelope properties]
                                     (if (not= msg {:direct "testing"})
                                       (println "Direct Publish Failure!!!"))))))))

(defn setup-test-queues []
  (thread-channel-off
   (fn [] (declare-queue "test-queue-direct" false false true)
     (bind-queue "test-queue-direct" "amq.direct" "test.direct")
       (direct-publish-test-consumer))))

(defn direct-publish-test-message []
  (thread-channel
   (fn []
     (try
       (doseq [i (range 0 100000)]
         (publish "amq.direct" "test.direct" (encode {:direct "testing"})))
       (catch Exception e
         (.printStackTrace e))))))

(defn consumer-failover-test []
  (consumer-failover "test-consumer-failover"
                     (create-consumer (fn [^bytes b-body props]
                                        (decode b-body))
                                      (fn [msg envelope properties]
                                        )))
  (Thread/sleep 1000)
  (common/close (:connection  @long-term/*connection*))
  (Thread/sleep 1000)
  (common/close (:connection  @long-term/*connection*))
  (println long-term/*consumers*))

(defn run-publish-test []
  (setup-test-queues)
  (direct-publish-test-message))

(defn run-queue-exists?-safe-test []
  (doseq [i (range 0 10)]
    (if (queue-exists?-safe "something-that-doesn't exists")
      (println "Queue exists didn't function correctly"))))

(defn start-tests
  "Used to start running all of the tests.  Assumes you want to connect on the local host.

username
  The name of the user to connect to the server with.
password
  The password to use to connect to the amqp server.
pool-size
  The size of the channel and thread pool to run the tests on."
  [username password pool-size]
  (setup-connection username password pool-size)
  (run-queue-exists?-safe-test)
  (consumer-failover-test))
