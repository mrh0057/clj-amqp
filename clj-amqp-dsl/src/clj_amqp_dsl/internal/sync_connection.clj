(ns clj-amqp-dsl.internal.sync-connection
  (:use clj-amqp-dsl.connection
        clj-amqp.common
        clj-amqp-dsl.internal.core
        clj-amqp.connection))

(defrecord ConnectionProtector [connection])

(defn make-connection-protector [connection]
  (ConnectionProtector. connection))

(def *connection*)

(defn setup-connection-listeners [connection]
  (add-shutdown-listener-to-connection connection
                                       (fn [reason]
                                         (println "Connection crashed in sync-connection!")
                                         (let [new-connection (create-connection)
                                               connection (swap! (fn [old-connection]
                                                                   (if (open? (:connection old-connection))
                                                                     old-connection
                                                                     (make-connection-protector new-connection))))]
                                           (if (not= new-connection (:connection connection))
                                             (close new-connection))))))

(defn initialize
  "Used to initialize the connection and other things necessary for the functions to perform their duty."
  []
  (let [connection (create-connection)]
    (setup-connection-listeners connection)
    (def *connection* (atom (make-connection-protector connection)))))

(defn with-temp-channel
  "Used to wrap the body with a temporary channel.  This function is used internally for use with operations
that may cause the channel to close.  This is not for use with function that may cause the connection close!
Make sure you your queue and exchanges exists before publishing message so that the connection doesn't get
dropped.

func
  The function to execute.
    The first agrument is the temporary channel.  You don't need to close the channel, the channel is closed for  you."
  [func]
  (let [channel (create-channel (:connection @*connection*))]
    (try
      (let [val (func channel)]
        (close channel)
        val)
      (catch Exception e
        (.printStackTrace e)
        (close channel)
        nil))))
