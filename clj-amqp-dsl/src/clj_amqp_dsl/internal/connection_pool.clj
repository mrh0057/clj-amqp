(ns clj-amqp-dsl.internal.connection-pool
  (:use clj-amqp-dsl.connection
        clj-amqp.connection
        clj-amqp.common)
  (:import org.apache.commons.pool.impl.GenericObjectPool
           [org.apache.commons.pool PoolableObjectFactory
            ObjectPool])
  (:require [clj-amqp.core :as amqp]))

(defrecord ConnectionInfo [connection])

(defn make-connection-info [connection]
  (ConnectionInfo. connection))

(defn- add-shutdown-notifier-to-connect [connection]
  (add-shutdown-notifier (:connection connection) (fn [reason]
                                        (.printStackTrace reason)
                                        (swap! connection (fn [old]
                                                            (assoc old :connection (create-connection))))
                                        (add-shutdown-notifier-to-connect @connection))))

(defprotocol ObjectPoolProtocol
  (borrow-object [this]
    "Used to borrow an object for the pool")
  (return-object [this obj]
    "Used to return an object to the pool

obj
  The object to return to the pool")
  (num-active [this]
    "Gets the number of active objects.")
  (num-idle [this]
    "Gets the number of idle objects."))

(extend-type ObjectPool
  ObjectPoolProtocol
  (borrow-object [this]
    (.borrowObject this))
  (return-object [this obj]
    (.returnObject this obj))
  (num-active [this]
    (.getNumActive this))
  (num-idle [this]
    (.getNumIdle this))
  Closable
  (close [this]
    (.close this)))

(defn create-channel-pool
  "Creates a general purpose connection pool for channel.

min
  The minimum number of channels to exists
max
  The maximum number of channels"
  [min max ]
  (let [connection (atom (make-connection-info (create-connection)))
        pool (GenericObjectPool. (proxy [PoolableObjectFactory] []
                                   (activateObject [channel])
                                   (destoryObject [channel]
                                     (println "Destroy object called this should never happen!!!")
                                     (println "Channel status " (open? channel))
                                     (close channel))
                                   (makeObject []
                                     (create-channel (:connection @connection)))
                                   (passivateObject [channel])
                                   (validateObject [channel]
                                     (open? channel)))
                                 2
                                 GenericObjectPool/WHEN_EXHAUSTED_BLOCK
                                 (* 30 1000)
                                 max
                                 min
                                 true
                                 true
                                 -1
                                 10
                                 -1
                                 true
                                 -1)]
    (add-shutdown-notifier-to-connect @connection)
    pool))

(defn with-pooled-channel [pooled-channel func]
  (let [channel (borrow-object pooled-channel)]
    (clj-amqp.core/with-channel channel
      (println "Inside with channel")
      (try
        (func)
        (finally
         (println "returning channel")
         (return-object pooled-channel channel))))))
