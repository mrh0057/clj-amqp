(ns clj-amqp-dsl.internal.unbound-channel-threads
  (:import [java.util.concurrent ConcurrentHashMap ExecutorService
            Executors ThreadPoolExecutor TimeUnit LinkedBlockingQueue]
           [java.util Date Map]
           clj_amqp_dsl.internal.ChannelThreadFactory)
  (:require [clj-amqp.channel :as channel]
            [clj-amqp.core :as amqp]
            [clj-amqp-dsl.connection :as dsl-conn]
            [clj-amqp.connection :as conn])
  (:use clj-amqp.common
        clj-amqp-dsl.internal.core))

(defrecord ConnectionInfo [connection
                           channels])

(defn make-connection-info  [connection
                             channels]
  (ConnectionInfo. connection
                   channels))

(def *clearing-thread-stop* false)

(declare *connection-info*)

(declare *thread-pool*)

(def *timeout* (* 5 60 1000))

(def *cleaning-period* (* 5 60 1000))

(declare *connection*)

(declare get-channel)

(defrecord ChannelInfo [thread-id
                        channel
                        timestamp])

(defn make-channel-info [thread-id channel]
  (ChannelInfo. thread-id
                channel
                (new Date)))

(defn execute-function [func]
  (.submit ^ExecutorService *thread-pool* ^Runnable (cast Runnable
                                                          (fn []
                                                            (try
                                                              (amqp/with-channel (get-channel)
                                                                (func))
                                                              (catch Exception e
                                                                (.printStackTrace e)))))))

(defn setup-connection-listeners [connection]
  (try
    (add-shutdown-notifier connection (fn [reason]
                                        (println "Connection removed!")
                                        (.printStackTrace ^Exception reason)
                                        (:connection (swap! *connection-info*
                                                (fn [old] {})))))
    (catch Exception e
      (println e)
      (.printStackTrace e))))

(defn get-connection []
  (if (not (:connection @*connection-info*))
    (let [new-connection (dsl-conn/create-connection)
          connection
          (swap! *connection-info*
                 (fn [old-connection]
                   (if (and (:connection old-connection)
                            (open? (:connection old-connection)))
                     old-connection
                     (make-connection-info new-connection
                                           (new java.util.concurrent.ConcurrentHashMap 300)))))]
      (if (not (= (:connection connection) new-connection))
        (close new-connection))
      connection)
    @*connection-info*))

(defn create-new-channel []
  (conn/create-channel (:connection (get-connection))))

(defn- get-channel-by-thread-id [thread-id]
  (let [current-channels (:channels (get-connection))]
    (if (contains? current-channels thread-id)
      (:channel (get current-channels thread-id))
      (let [channel (make-channel-info thread-id (create-new-channel))]
        (.put ^Map current-channels thread-id channel)
        (:channel channel)))))

(defn get-channel
  "Used to get a channel for the corresponding thread.

returns
  The channel for the thread."
  []
  (get-channel-by-thread-id (.getId (Thread/currentThread))))

(defn remove-old-channels
  "Used to remove old channels."
  [channels]
  (println "Removing old channels.")
  (let [date (.getTime (new Date))]
    (doseq [channel (.values ^Map channels)]
      (if (> date (+ *timeout* (.getTime ^Date (:timestamp channel))))
        (do
          (try
            (close (:channel channel))
            (catch Exception e
              (println e)
              (.printStackTrace e)))
            (.remove ^Map channels (:thread-id channel)))))))

(defn- clean-thread []
  (loop []
    (if (not *clearing-thread-stop*)
      (do
        (. Thread (sleep *cleaning-period*))
        (println "Looking for old channels...")
        (if-let [channels (:channels @*connection-info*)]
          (remove-old-channels channels))
        (recur)))))

(defn- start-clearing-thread []
  (println "Staring clearing thread...")
  (future (clean-thread)))

(defn initialize []
  (def *thread-pool* (Executors/newCachedThreadPool (new ChannelThreadFactory)))
  (def *connection-info* (atom {}))
  (start-clearing-thread))
