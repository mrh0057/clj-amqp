;; Contains the core functionality for the dsl.
(ns clj-amqp-dsl.core
  (:require [clj-amqp-dsl.internal.sync-connection :as sync-connection]
            [clj-amqp.channel :as channel]
            [clj-amqp-dsl.internal.long-term-connection :as long-term]
            [clj-amqp-dsl.internal.unbound-channel-threads :as unbound]
            [clj-amqp-dsl.internal.channel-threads :as channel-threads])
  (:use clj-amqp.core
        clj-amqp-dsl.connection))

(defn thread-channel
  "Wraps the body of the expression and executes it on the thread pool where it has a channel bound to that thread.
The functions passed is executed in a separate thread.  So when it returns your function has been
executed yet

`func`
  The function to execute. This function will execute in a separate thread and is wrap with a channel that is
  bound to that thread."
  [func]
  (clj-amqp-dsl.internal.channel-threads/execute-function
    (fn []
      (func))))

(defn number-of-processors
  "Used to get the number of processor for the machine."
  []
  (.availableProcessors (Runtime/getRuntime)))

(defn thread-channel-off
  "Executes the function in a thread pool that is unbound.  Use this function if your function isn't cpu bound.
Each thread has a dedicated channel.

`func`
  The function to execute in the unbount thread pool"
  [func]
  (unbound/execute-function func))

(defn consumer-failover
  "Used to create a consumer that if something where to happen to the connection it will create 
a new connection and add the consumer immediately back to the message pool.  The queue must be 
durable ie. survives after the channel/connection closes.  If you don't do this your code will
be in a constant loop of trying to establish a new connection constantly.  You can't remove
a consumer when you use this function.

The consumer create with this function using a different connection then the ones created within
async messaging.

Exceptions are printed to stdin for now.  May move to a logging library later so you have the ability to log to a file.

`queue`
  The durable queue to consume.

`consumer`
  The consumer to add to the messaging pool."
  [queue consumer]
  (long-term/add-consumer-failover queue consumer))

(defn- create-consumer-thread
  [decoder handler msg-checker msg-rejection thread-function]
  (fn [channel body envelope properties]
    (try
      (let [msg (decoder body properties)]
        (with-channel channel
          (if (msg-checker msg envelope properties)
            (acknowledge (:delivery-tag envelope))
            (msg-rejection msg envelope properties)))
        (thread-function
         (fn []
           (try 
             (handler msg envelope properties)
             (catch Exception e
               (println e)
               (.printStackTrace e))))))
      (catch Exception e
        (println e)
        (.printStackTrace e)))))

(defn create-consumer
    "Used to create a consumer.  Must use this method to create consumer with the dsl or you
will get errors like delivery tag unknown.  Consumer use the cpu bound thread pool.

`decoder`
  The decoder to use to decode the messages.
  The decoder is passed the binary body and the properties for the message.

`message-processor`
  The function that process the incoming message.
  Takes 3 arguments
    message    The decoded body.
    envelope   The envelope for the message.
    properties The message properites.

`msg-checker`
  If it return false, then msg-rejection is called.  The default implementation acknowledges the message
   Takes 3 arguments: 
     message    The decoded message body.
     envelope   The envelope for the message.
     properties The message properties.

`msg-rejection`
  The function to call if you reject the message.  The default implementations sends a rejection response
   with no requeue.
   Takes 3 arguments: 
     message    The decoded message body.
     envelope   The envelope for the message.
     properties The message properties."
    ([decoder handler]
       (create-consumer decoder handler (fn [msg envelope properties] true)))
    ([decoder handler msg-checker]
       (create-consumer decoder handler msg-checker (fn [msg envelope properties]
                                                      (reject (:delivery-tag envelope) false))))
    ([decoder handler msg-checker msg-rejection]
       (create-consumer-thread decoder handler msg-checker msg-rejection clj-amqp-dsl.internal.channel-threads/execute-function)))

(defn create-consumer-off
  "Used to create a consumer.  Must use this method to create consumer with the dsl or you
will get errors like delivery tag unknown.  Consumer use the unbound thread pool to process incoming messages.

`decoder`
  The decoder to use to decode the messages.
    The decoder is passed the binary body and the properties for the message.

`message-processor`
  The function that process the incoming message.
  Takes 3 arguments
    message    The decoded body.
    envelope   The envelope for the message.
    properties The message properites.

`msg-checker`
  If it return false, then msg-rejection is called.  The default implementation acknowledges the message
   Takes 3 arguments: 
   `message`    The decoded message body.
   `envelope`   The envelope for the message.
   `properties` The message properties.

`msg-rejection`
  The function to call if you reject the message.  The default implementations sends a rejection response
  with no requeue.
  Takes 3 arguments: 
  message    The decoded message body.
  envelope   The envelope for the message.
  properties The message properties."
  ([decoder handler]
     (create-consumer decoder handler (fn [msg envelope properties] true)))
  ([decoder handler msg-checker]
     (create-consumer decoder handler msg-checker (fn [msg envelope properties]
                                                    (reject (:delivery-tag envelope) false))))
  ([decoder handler msg-checker msg-rejection]
     (create-consumer-thread decoder handler msg-checker msg-rejection unbound/execute-function)))

(defn start
  "Starts the connection to the server.

`connection-factory`
  The factory to use to create new connection to the server.
`pool-size`
  The number of channels and threads to use in the pool.
  For now all of the channels in this pool use the same connection.  If you have performance problems you may
  want to try creating your own thread pool that also has a pool of channels.  Later realse will most likely
  move to this model.
  You can use the function number-of-processors to get the number of processors for you system to decide on the
  pool-size.  For cpu bound task you should use n+2."
  [connection-factory pool-size]
  (do
    (set-create-connection-factory connection-factory)
    (channel-threads/initialize pool-size)
    (sync-connection/initialize)
    (long-term/initialize)
    (unbound/initialize)))

(defn queue-exists?-safe
  "Used to check if a queue exists in a separate channel so that it doesn't close the channel you are working on.

`queue`
  The name of the queue to see if it exists."
  [queue]
  (sync-connection/with-temp-channel
    (fn [chan]
      (channel/queue-exists? chan queue))))
