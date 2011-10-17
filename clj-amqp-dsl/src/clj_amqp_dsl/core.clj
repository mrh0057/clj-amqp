(ns clj-amqp-dsl.core
  (:use clj-amqp.core
        clj-amqp-dsl.connection
        [clj-amqp-dsl.internal.channel-threads :only [initialize]]))

(defmacro amqp-async-messaging
  "Wraps the body of the expression and executes it on the thread pool where it is set a channel.

The message will get processed asynchronous so when you function return the message has NOT be processed yet.

Do not use the consumer function from the amqp api.  Use one of the provider consumer function.  All operations with the
body must be completed within 5 minutes. If it has been completed with 5 minutes the channel may be closed.
Do not do any long term blocking operations or you server may crash because of all threads that will be spawned.

body
  The body of the expression to execute."
  [& body]
  `(clj-amqp-dsl.internal.channel-threads/execute-function
    (fn []
      (clj-amqp.core/with-channel (clj-amqp-dsl.internal.channel-threads/get-channel)
        ~@body))))

(defmacro create-consumer
  "Used to create a consumer.

decoder
  The decoder to use to decode the messages.
    The decoder is passed the binary body and the properties for the message.
body
  The body of the message to execute.
   The variables:
     msg The decode msg.
     envelope The evelope for the message.
     properites The properties of the message."
  [decoder & body]
  `(fn [body# ~'envelope ~'properties]
    (try
      (clj-amqp-dsl.internal.channel-threads/execute-function
       (fn []
         (let [~'msg (~decoder body# ~'properties)]
           (clj-amqp.core/with-channel (clj-amqp-dsl.internal.channel-threads/get-channel)
             ~@body))))
      (catch Exception ~'e
        (println ~'e)))))

(defn start
  "Must call this before you perform any operations on the server.

connection-factory
  The factory to use to create new connection to the server."
  [connection-factory]
  (set-create-connection-factory connection-factory)
  (initialize))
