(ns clj-amqp-dsl.internal.core
  (:use clj-amqp.common))

(defn add-shutdown-listener-to-connection [connection func]
  (add-shutdown-notifier connection
                         (fn [reason]
                           (.printStackTrace ^Exception (:exception reason))
                           (func reason))))
