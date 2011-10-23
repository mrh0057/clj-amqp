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

(def shutdown-handler)

(defn- update-connection []
  (swap! *connection*
         (fn [connection-info]
           (if (not (open? (:connection connection-info)))
             (let [connection (create-connection)]
               (doseq [consumer-queue @*consumers*]
                 (add-consumer-to-harden connection
                                         (:queue consumer-queue)
                                         (:consumer consumer-queue))
                 (add-shutdown-listener-to-connection connection shutdown-handler))
               (assoc connection-info :connection connection))
             connection-info))))

(defn shutdown-handler [reason]
  (do
    (.printStackTrace ^Exception (:exception reason))
    (update-connection)))

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

(defn add-consumer-to-harden [connection queue consumer-processor]
  (if (open? connection)
    (let [consumer (consumer connection queue consumer-processor)]
      (add-shutdown-listener-to-connection consumer
                                           channel-shutdown-listener))))

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
       (if (not (open? (:connection @*connection*)))
         (update-connection))
       (add-consumer-to-harden (:connection @*connection*) queue consumer-processor)
       (cons (make-consumer-queue consumer queue) val)
       (catch Exception e
         (.printStackTrace e)
         val)))))
