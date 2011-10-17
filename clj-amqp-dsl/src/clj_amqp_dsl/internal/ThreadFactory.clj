(ns clj-amqp-dsl.internal.ThreadFactory
  (:gen-class
   :name clj_amqp_dsl.internal.ChannelThreadFactory
   :implements [java.util.concurrent.ThreadFactory]))

(def *thread-group* (new ThreadGroup "amqp-thread-workers"))

(defn -newThread [this runnable]
  (Thread. #^ThreadGroup *thread-group* #^Runnable runnable))
