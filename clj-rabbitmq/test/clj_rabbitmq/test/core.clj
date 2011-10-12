(ns clj-rabbitmq.test.core
  (:use [clj-rabbitmq.core] :reload)
  (:use clj-amqp.connection
        clj-amqp.channel)
  (:use [clojure.test]))

(defn- get-test-connection []
  (connect {:host "localhost"
            :username "guest"
            :password "guest"
            :port 5672
            :request-heartbeat (* 5 60 1000)
            :request-channel-max 10000}))

(deftest connect-test
  (let [connection (connect {:host "localhost"
                             :username "guest"
                             :password "guest"
                             :port 5672
                             :request-heartbeat (* 5 60 1000)
                             :request-channel-max 10000})]
    (.close connection)))

(deftest create-channel-test
  (let [connection (get-test-connection)
        channel (create-channel connection)
        channel-timeout (create-channel connection 100)]
    (.close channel)
    (.close channel-timeout)
    (.close connection)))

(deftest connection-abort-test
  (let [connection (get-test-connection)]
    (address connection)
    (.close connection)))

(deftest connection-close-test
  (let [connection (get-test-connection)]
    (close connection)
    (close (get-test-connection) 3)))

(deftest connection-channel-max-test
  (let [connection (get-test-connection)]
    (is (> (channel-max connection) 0))
    (.close connection)))

(deftest connection-frame-max-test
  (let [connection (get-test-connection)]
    (is (> (frame-max connection) 0))
    (.close connection)))

(deftest connection-port-test
  (let [connection (get-test-connection)]
    (is (> (port connection) 0))
    (.close connection)))

(deftest connection-port-test
  (let [connection (get-test-connection)
        channel (create-channel connection)]
    (is (string? (exclusive-queue channel)))
    (is (string? (declare-queue channel "my.queue" false true true)))
    (declare-queue channel "myqueue-delete" false true true)
    (delete-queue channel "my.queue")
    (delete-queue channel "myqueue-delete" {:unused true :empty true})
    (.close channel)
    (.close connection)))


(deftest purge-queue-test
  (let [connection (get-test-connection)
        channel (create-channel connection)
        queue (exclusive-queue channel)]
    (purge-queue channel queue)
    (.close channel)
    (.close connection)))

(deftest consumer-test
  (with-open [connection (get-test-connection)
              channel (create-channel connection)]
    (declare-queue channel "consumer.test" false true true)
    (consumer channel "consumer.test" (fn [body envelope]))))

(deftest exchange-test
  (with-open [connection (get-test-connection)
              channel (create-channel connection)]
    (exchange channel "my.exchange" :direct)
    (exchange channel "my.exchange" :direct {:durable false
                                             :auto-delete false
                                             :internal false})))
