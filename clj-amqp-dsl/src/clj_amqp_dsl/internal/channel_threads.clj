(ns clj-amqp-dsl.internal.channel-threads
  (:import [java.util.concurrent ConcurrentHashMap
            Executors ThreadPoolExecutor TimeUnit LinkedBlockingQueue]
           [java.util Date]
           clj_amqp_dsl.internal.ChannelThreadFactory)
  (:use [clj-amqp-dsl.connection :only [create-connection]]
        clj-amqp.common
        clj-amqp-dsl.internal.core
        clj-amqp-dsl.internal.connection-pool
        [clj-amqp.connection :only [create-channel]])
  (:require [clj-amqp.channel :as channel]))

(def *thread-pool*)

(def *channel-pool*)

(defn execute-function [func]
  (.submit *thread-pool* (fn []
                           (with-pooled-channel *channel-pool*
                             func))))

(defn initialize []
  (def *thread-pool* (Executors/newFixedThreadPool 1000 (new ChannelThreadFactory)))
  (def *channel-pool* (create-channel-pool 1000 1000)))
