(ns clj-amqp-dsl.internal.channel-threads
  (:import [java.util.concurrent ConcurrentHashMap
            Executors ThreadPoolExecutor TimeUnit LinkedBlockingQueue]
           [java.util Date]
           clj_amqp_dsl.internal.ChannelThreadFactory)
  (:use [clj-amqp-dsl.connection :only [create-connection]]
        [clj-amqp.connection :only [create-channel add-shutdown-notifier open?]]))

(defrecord ConnectionInfo [connection
                           channels])

(defn make-connection-info  [connection
                             channels]
  (ConnectionInfo. connection
                   channels))

(def *clearing-thread-stop* false)

(def *connection-info*)

(def *thread-pool*)

(def *timeout* (* 5 60 1000))

(def *cleaning-period* (* 5 60 1000))

(def *connection* nil)

(defrecord ChannelInfo [thread-id
                        channel
                        timestamp])

(defn make-channel-info [thread-id channel]
  (ChannelInfo. thread-id
                channel
                (new Date)))

(defn remove-all-channels []
  (def *current-channels* (new java.util.concurrent.ConcurrentHashMap 300)))

(defn execute-function [func]
  (.submit *thread-pool* func))

(defn setup-connection-listeners [connection]
  (try
    (add-shutdown-notifier connection (fn [reason]
                                        (println "Connection removed!")
                                        (:connection (swap! *connection-info*
                                                {}))))
    (catch Exception e
      (println e))))

(defn get-connection []
  (if (not (:connection @*connection-info*))
    (let [new-connection-info (make-connection-info (create-connection)
                                                    (new java.util.concurrent.ConcurrentHashMap 300))]
      (let [connection (swap! *connection-info*
                              (fn [old-connection]
                                (if (and (:connection old-connection)
                                         (open? (:connection old-connection)))
                                  old-connection
                                  new-connection-info)))]
        (if (not= @*connection-info* new-connection-info)
          (.close (:connection new-connection-info)))
        connection))
    @*connection-info*))

(defn create-new-channel []
  (println "creating a new channel")
  (create-channel (:connection (get-connection))))

(defn- get-channel-by-thread-id [thread-id]
  (let [current-channels (:channels (get-connection))]
    (if (contains? current-channels thread-id)
      (get current-channels thread-id)
      (let [channel (make-channel-info thread-id (create-new-channel))]
        (.put current-channels thread-id channel)
        (:channel channel)))))

(defn get-channel
  "Used to get a channel for the corresponding thread.

returns
  The channel for the thread."
  []
  (let [thread-id (.getId (Thread/currentThread))]
    (get-channel-by-thread-id thread-id)))

(defn remove-old-channels
  "Used to remove old channels."
  []
  (println "String to remove old channels.")
  (if-let [map (:channels @*connection-info*)]
      (let [date (.getTime (new Date))]
        (doseq [channel (.values map)]
          (if (< date (+ *timeout* (.getTime (:timestamp channel))))
            (try
              (.close (:channel channel))
              (catch Exception e
                (println e)))
           (.remove map (:thread-id map)))))))

(defn- clean-thread []
  (loop []
    (if (not *clearing-thread-stop*)
      (do
        (. Thread (sleep *cleaning-period*))
        (println "Looking for old channels...")
        (remove-old-channels)
        (recur)))))

(defn- start-clearing-thread []
  (println "Staring clearing thread...")
  (future (clean-thread)))

(defn initialize []
  (def *thread-pool* (Executors/newCachedThreadPool (new ChannelThreadFactory)))
  (def *connection-info* (atom {}))
  (start-clearing-thread)
  (remove-all-channels))
