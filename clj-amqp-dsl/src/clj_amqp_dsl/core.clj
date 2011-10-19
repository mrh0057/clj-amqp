(ns clj-amqp-dsl.core
  (:use clj-amqp.core
        clj-amqp-dsl.connection
        [clj-amqp-dsl.internal.channel-threads :only [initialize]])
  (:require [clj-amqp-dsl.internal.sync-connection :as sync-connection]
            [clj-amqp.channel :as channel]
            [clj-amqp-dsl.internal.long-term-connection :as long-term]))

(defn amqp-async-messaging
  "Wraps the body of the expression and executes it on the thread pool where it has a channel set.

The functions passed is executed in a separate thread.  So when it returns your function has been
executed yet!

Do not use the consumer function from the amqp api.  Use one of the provider consumer function. 

func
  The function to execute. This function will execute in a separate thread and is wrap with a channel that is
bound to that thread."
  [func]
  (clj-amqp-dsl.internal.channel-threads/execute-function
    (fn []
      (func))))

(defn consumer-failover
  "Used to create a consumer that if something where to happen to the connection it will create 
a new connection and add the consumer immediately back to the message pool.  The queue must be 
durable (ie. survives after the channel/connection closes.  If you don't do this your code will
be in a constant loop of trying to establish a new connection constantly.  You can't remove
a consumer when you use this function.

The consumer create with this function using a different connection then the ones created within
async messaging.

Exceptions are printed to stdin for now.  May move to a logging library later so you have the ability to log to a file.

queue
  The durable queue to consume.
consumer
  The consumer to add to the messaging pool."
  [queue consumer]
  (long-term/add-consumer-failover queue consumer))

(defn consumer-with-failover-handler
  "Used to consume a message and adds a your channel failure listener at the same time.

queue
  The queue for your consumer to process.
consumer
  The consumer function
fail-handler
  The handler to be called in the event of a channel failure."
  [queue consumer fail-handler]
  (long-term/add-consumer-custom-failure-handler queue consumer fail-handler))

(defn create-consumer
  "Used to create a consumer.

decoder
  The decoder to use to decode the messages.
    The decoder is passed the binary body and the properties for the message.
message-processor
  The function that process the incoming message.
    Takes 3 arguments
      message    The decoded body.
      envelope   The envelope for the message.
      properties The message properites."
  [decoder handler]
  (fn [body envelope properties]
    (try
      (clj-amqp-dsl.internal.channel-threads/execute-function
       (fn []
         (try 
           (let [msg (decoder body properties)]
             (handler msg envelope properties))
           (catch Exception e
             (println e)
             (.printStackTrace e)))))
      (catch Exception e
        (println e)
        (.printStackTrace e)))))

(defn start
  "Starts the connection to the server.

connection-factory
  The factory to use to create new connection to the server."
  [connection-factory]
  (set-create-connection-factory connection-factory)
  (initialize)
  (sync-connection/initialize)
  (long-term/initialize))

(defn queue-exists?-safe
  "Used to check if a queue exists in a separate channel so that it doesn't close the channel you are working on.

queue 
  The name of the queue to see if it exists."
  [queue]
  (sync-connection/with-temp-channel
    (fn [chan]
      (channel/queue-exists? chan queue))))
