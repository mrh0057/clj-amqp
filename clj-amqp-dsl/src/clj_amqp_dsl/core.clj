(ns clj-amqp-dsl.core
  (:use clj-amqp.core
        clj-amqp-dsl.connection
        [clj-amqp-dsl.internal.channel-threads :only [initialize]]))

(defn amqp-async-messaging
  "Wraps the body of the expression and executes it on the thread pool where it has a channel set.

The message will get processed asynchronous so when you function return the message has NOT be processed yet.

Do not use the consumer function from the amqp api.  Use one of the provider consumer function.  All operations with the
body must be completed within 5 minutes. If it hasn't been completed with 5 minutes the channel may be closed.
Do not do any long term blocking operations or you server may crash because of all threads that will be spawned.  Create your
own thread pool if you want to do this.

func
  The function to execute. This function will execute in a separate thread and is wrap with a channel."
  [func]
  (clj-amqp-dsl.internal.channel-threads/execute-function
    (fn []
      (clj-amqp.core/with-channel (clj-amqp-dsl.internal.channel-threads/get-channel)
        (func)))))

(defn create-consumer
  "Used to create a consumer.

decoder
  The decoder to use to decode the messages.
    The decoder is passed the binary body and the properties for the message.
message-processor
  The function that process the incoming message.
    Takes 3 arguments
      message  The decoded body.
      envelope The envelope for the message.
      properties The message properites."
  [decoder handler]
  (fn [body envelope properties]
    (try
      (clj-amqp-dsl.internal.channel-threads/execute-function
       (fn []
         (try 
           (let [msg (decoder body properties)]
             (clj-amqp.core/with-channel (clj-amqp-dsl.internal.channel-threads/get-channel)
               (handler msg envelope properties)))
           (catch Exception e
             (println e)
             (.printStackTrace e)))))
      (catch Exception e
        (println e)
        (.printStackTrace e)))))

(defn start
  "Must call this before you perform any operations on the server.

connection-factory
  The factory to use to create new connection to the server."
  [connection-factory]
  (set-create-connection-factory connection-factory)
  (initialize))
