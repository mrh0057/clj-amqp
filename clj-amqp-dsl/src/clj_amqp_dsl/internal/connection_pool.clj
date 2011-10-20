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
  (add-shutdown-notifier (:connection @connection)
                         (fn [reason]
                           (.printStackTrace ^Exception (:exception reason))
                           (swap! connection (fn [old]
                                               (assoc old :connection (create-connection))))
                           (add-shutdown-notifier-to-connect connection))))

(defrecord ObjectPoolWithConnection [^ObjectPool object-pool connection])

(defn- make-object-pool-with-connection [^ObjectPool object-pool connection]
  (ObjectPoolWithConnection. object-pool connection))

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

(extend-type ObjectPoolWithConnection
  ObjectPoolProtocol
  (borrow-object [this]
    (.borrowObject ^ObjectPool (:object-pool this)))
  (return-object [this obj]
    (.returnObject ^ObjectPool (:object-pool this) obj))
  (num-active [this]
    (.getNumActive ^ObjectPool (:object-pool this)))
  (num-idle [this]
    (.getNumIdle ^ObjectPool (:object-pool this)))
  Closable
  (close [this]
    (.close ^ObjectPool (:object-pool this))
    (close @(:connection this))))

(defn create-channel-pool
  "Creates a general purpose connection pool for channel.

min
  The minimum number of channels to exists
max
  The maximum number of channels"
  [min max ]
  (let [connection (atom (make-connection-info (create-connection)))
        pool (make-object-pool-with-connection
               (GenericObjectPool. (proxy [PoolableObjectFactory] []
                                     (activateObject [channel])
                                     (destoryObject [channel]
                                       (println "Destroy object called this should never happen!!!")
                                       (println "Channel status " (open? channel))
                                       (close channel))
                                     (makeObject []
                                       (println "making new channel")
                                       (create-channel (:connection @connection)))
                                     (passivateObject [channel])
                                     (validateObject [channel]
                                       (if (not (open? channel))
                                         (.printStackTrace (.getCloseReason channel)))
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
                                   -1)
               connection)]
    (add-shutdown-notifier-to-connect connection)
    pool))

(defn with-pooled-channel [pooled-channel func]
  (let [channel (borrow-object pooled-channel)]
    (clj-amqp.core/with-channel channel
      (try
        (func)
        (finally
         (println "returning to pool")
         (return-object pooled-channel channel))))))
