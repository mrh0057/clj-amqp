(ns clj-rabbitmq.internal.core
  (:use clj-amqp.channel
        clj-rabbitmq.internal.constants)
  (:import com.rabbitmq.client.AMQP$BasicProperties))

(defn convert-message-properites [^com.rabbitmq.client.AMQP$BasicProperties message-properties]
  (make-message-properties (.getClassId message-properties)
                           (.getContentType message-properties)
                           (.getContentEncoding message-properties)
                           (.getHeaders message-properties)
                           (.getDeliveryMode message-properties)
                           (.getPriority message-properties)
                           (.getCorrelationId message-properties)
                           (.getReplyTo message-properties)
                           (.getExpiration message-properties)
                           (.getMessageId message-properties)
                           (.getTimestamp message-properties)
                           (.getType message-properties)
                           (.getUserId message-properties)
                           (.getAppId message-properties)
                           (.getClusterId message-properties)))

(defn convert-options-to-basic-properties [options]
  (new AMQP$BasicProperties
       (:content-type options)
       (:content-encoding options)
       (:headers options)
       (cond (= :persistent (:delivery-mode options))
             *delivery-persistent*
             (= :nonpersitent (:delivery-mode options))
             *delivery-no-persistent*
             :else
             nil)
       (:priority options)
       (:correlation-id options)
       (:reply-to options)
       (:expiration options)
       (:message-id options)
       (:timestamp options)
       (:type options)
       (:user-id options)
       (:app-id options)
       (:cluster-id options)))
