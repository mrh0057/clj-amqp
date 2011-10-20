(ns clj-amqp-dsl.internal.long-term-connection
  (:use clj-amqp.channel
        clj-amqp.connection
        clj-amqp-dsl.connection
        clj-amqp-dsl.internal.core
        clj-amqp.common))

(defrecord ConnectionInfo [connection])

(defn make-connection-info [connection]
  (ConnectionInfo. connection))

(defrecord ConsumerQueue [consumer
                          queue])

(defn make-consumer-queue [consumer queue]
  (ConsumerQueue. consumer queue))

(def *consumers*)

(def *connection*)

(def setup-connection-listeners)

(def add-consumer-to-harden)

(defn- shutdown-handler [reason]
  (let [connection (create-connection)]
    (.printStackTrace ^Exception (:exception reason))
    (swap! *connection* (fn [connection-info]
                          (assoc connection-info :connection connection)))
    (doseq [consumer-queue @*consumers*]
      (println consumer-queue)
      (add-consumer-to-harden (:queue consumer-queue)
                              (:consumer consumer-queue)))
    ;; do this at the end to prevent an endless cycle if something
    ;; went wrong
    (add-shutdown-listener-to-connection connection shutdown-handler)))

(defn setup-connection-listeners [connection]
  (add-shutdown-listener-to-connection connection shutdown-handler))

(defn initialize []
  (let [connection (create-connection)
        connection-weak (create-connection)]
    (def *connection* (atom (make-connection-info connection)))
    (def *connection-weak* (atom (make-connection-info connection-weak)))
    (def *consumers* (agent '()))
    (setup-connection-listeners connection)))

(defn- channel-shutdown-listener [queue consumer-handler]
  (fn [reason]
    (if (not (:hard-error reason))
      (let [channel (create-channel (:connection @*connection*))]
        (consumer channel queue consumer-handler)
        ;; Call after just incase there is an error somewhere in the consumer.
        (add-shutdown-listener-to-connection channel
                                             channel-shutdown-listener)))))

(defn add-consumer-to-harden [queue consumer-processor]
  (let [consumer (consumer (:connection @*connection*) queue consumer-processor)]
    (add-shutdown-listener-to-connection consumer
                                         channel-shutdown-listener)))

(defn add-consumer-failover
  "Used to add a consumer to an individual channel.  This is done to prevent any type of conflicts with
threads and to make it impossible for one channel failure to affect another.  It is assumed that if
you plan to use this function these are vital for the operation of your program and should be kept
consuming as much as possible.

queue
  The name of the queue for the consumer to consume.
consumer-processor
  The consumer to consume the messages."
  [queue consumer-processor]
  (send-off *consumers*
   (fn [val]
     (try
       (add-consumer-to-harden queue consumer-processor)
       (cons (make-consumer-queue consumer queue) val)
       (catch Exception e
         (.printStackTrace e)
         val)))))
