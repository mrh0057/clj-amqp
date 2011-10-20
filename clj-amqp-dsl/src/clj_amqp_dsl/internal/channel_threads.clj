(ns clj-amqp-dsl.internal.channel-threads
  (:import [java.util.concurrent ConcurrentHashMap ExecutorService
            Executors ThreadPoolExecutor TimeUnit LinkedBlockingQueue]
           [java.util Date]
           clj_amqp_dsl.internal.ChannelThreadFactory)
  (:use [clj-amqp-dsl.connection :only [create-connection]]
        clj-amqp.common
        clj-amqp-dsl.internal.core
        clj-amqp-dsl.internal.connection-pool
        [clj-amqp.connection :only [create-channel]])
  (:require [clj-amqp.channel :as channel]
            [clj-amqp.core :as amqp]))

(def ^ExecutorService *thread-pool*)

(def *channel-pool*)

(defn execute-function [func]
  (.submit *thread-pool* 
           ^Runnable (cast Runnable (fn []
                                      (try
                                        (with-pooled-channel *channel-pool*
                                          func)
                                        (catch Exception e
                                          (.printStackTrace e)))))))

(defn execute-function-with-channel [channel func]
  (.submit *thread-pool*
           ^Runnable (cast Runnable (fn []
                                      (try
                                        (amqp/with-channel channel
                                          (func))
                                        (catch Exception e
                                          (.printStackTrace e)))))))

(defn initialize [pool-size]
  (def *thread-pool* (Executors/newFixedThreadPool pool-size (new ChannelThreadFactory)))
  (def *channel-pool* (create-channel-pool pool-size pool-size)))
