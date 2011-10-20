(ns clj-amqp-dsl.internal.ThreadFactory
  (:gen-class
   :name clj_amqp_dsl.internal.ChannelThreadFactory
   :implements [java.util.concurrent.ThreadFactory]))

(def *thread-group* (new ThreadGroup "amqp-thread-workers"))

(defn -newThread [this runnable]
  (let [thread (Thread. #^ThreadGroup *thread-group* #^Runnable runnable)]
    (.setUncaughtExceptionHandler thread (proxy [Thread$UncaughtExceptionHandler] []
                                           (uncaughtException [t ^Exception e]
                                             (println "Warning an exception escape a thread and should have been caught.")
                                             (.printStackTrace e))))
    thread))
