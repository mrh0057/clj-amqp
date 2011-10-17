(ns clj-amqp-dsl.test.core
  (:use [clj-amqp-dsl.core] :reload
        clj-rabbitmq.core
        clj-amqp-dsl.internal.channel-threads
        clj-amqp-dsl.connection
        clj-bson.core)
  (:use [clojure.test]))


(defn connection-create-func []
  (connect {:host "localhost"}))

(start connection-create-func)

(deftest create-consumer-test
  (let [body {:a "body"}]
    (doseq [x (range 0 10000)]
      ((create-consumer (fn [^bytes body props]
                          (decode body))
                        (println "Thread went to sleep")
                        (println "Executed.....")
                        (println "The number of channels is: " (.size (:channels @*connection-info*)))) (encode body) "b" "c"))))

(deftest check-current-channels-test)

(println (.getPoolSize *thread-pool*))

(println "Connection: " *connection*)
