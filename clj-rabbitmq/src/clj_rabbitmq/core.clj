(ns clj-rabbitmq.core
  (:use clj-rabbitmq.internal.options
        clj-rabbitmq.internal.constants
        clj-rabbitmq.internal.core
        clj-amqp.common
        clj-amqp.connection
        clj-amqp.channel)
  (:import [com.rabbitmq.client ConnectionFactory
            Connection
            Channel
            ShutdownSignalException
            DefaultConsumer
            ShutdownListener
            ShutdownNotifier
            AMQP]
           com.rabbitmq.client.AMQP$BasicProperties
           com.rabbitmq.client.AMQP$Queue$DeclareOk
           [java.util HashMap])
  (:require [clojure.string :as str-utils]))

(defn create-shotdown-listener-proxy [function-handler]
  (proxy [ShutdownListener] []
    (shutdownCompleted [^ShutdownSignalException cause]
      (function-handler (make-shutdown-signal-info
                         cause
                         (.getReason cause)
                         (.getReference cause)
                         (.isHardError cause)
                         (.isInitiatedByApplication cause))))))

(defn- convert-declare-queue-ok [^AMQP$Queue$DeclareOk info]
  (make-queue-info (.getQueue info)
                   (.getConsumerCount info)
                   (.getMessageCount info)))

(defn- convert-envelope [^com.rabbitmq.client.Envelope envelope]
  (make-envelope (.getDeliveryTag envelope)
                 (.getExchange envelope)
                 (.getRoutingKey envelope)
                 (.isRedeliver envelope)))

(defn- create-consumer-proxy [channel consumer]
  (proxy [DefaultConsumer] [channel]
    (handleCancel [consumer-tag]
      (close channel))
    (handleCancelOk [consumer-tag])
    (handleConsumeOk [consumer-tag])
    (handleDelivery [consumer-tag ^com.rabbitmq.client.Envelope envelope properties ^bytes body]
      (consumer channel
                body
                (convert-envelope envelope)
                (convert-message-properites properties)))
    (handleRecoverOk [])
    (handleShutdownSignal [consumerTag sig])))

(extend-type Connection
  ConnectionProtocol
  (create-channel
    ([this]
       (.createChannel this))
    ([this number]
       (.createChannel this number)))
  (address [this]
    (.getAddress this))
  (close-with-timeout
    ([this timeout]
       (.close this timeout)))
  (channel-max [this]
    (.getChannelMax this))
  (frame-max [this]
    (.getFrameMax this))
  (heart-beat [this]
    (.getHeartbeat this))
  (port [this]
    (.getPort this))
  ConsumerCreator
  (consumer [this queue consumer]
    (let [channel ^Channel (create-channel this)]
      (make-consumer-info (.basicConsume channel queue false (create-consumer-proxy channel consumer))
                          channel)))
  Closable
  (close [this]
    (if (open? this)
      (.close this))))

(extend-type Channel
  ChannelProtocol
  (basic-get [this queue auto-ack]
    (let [^com.rabbitmq.client.GetResponse response (.basicGet this queue auto-ack)]
      (make-get-response (.getBody response)
                         (convert-envelope (.getEnvelope response))
                         (.getMessageCount response)
                         (convert-message-properites (.getProps response)))))
  (queue-exists? [this queue]
    (try
      (convert-declare-queue-ok(.queueDeclarePassive this queue))
      (catch Exception e
        false)))
  (exclusive-queue [this]
    (.getQueue (.queueDeclare this)))
  (bind-queue ([this queue exchange routing-key]
                 (bind-queue this queue exchange routing-key {}))
    ([this queue exchange routing-key arguments]
     (.queueBind this queue exchange routing-key arguments)))
  (unbind-queue ([this queue exchange routing-key]
                   (unbind-queue this queue exchange routing-key {}))
    ([this queue exchange routing-key arguments]
       (.queueUnbind this queue exchange routing-key)))
  (declare-queue ([this queue durable exclusive auto-delete]
                    (declare-queue this queue durable exclusive auto-delete {}))
    ([this queue durable exclusive auto-delete arguments]
     (.getQueue (.queueDeclare this queue durable exclusive auto-delete arguments))))
  (delete-queue
    ([this queue]
       (.queueDelete this queue))
    ([this queue options]
       (.queueDelete this queue
                     (if (:unused queue)
                       (:unused queue)
                       false)
                     (if (:empty queue)
                       (:empty queue)
                       false))))
  (purge-queue [this queue]
    (.queuePurge this queue))
  (acknowledge [this delivery-tag]
    (.basicAck this delivery-tag false))
  (reject ([this delivery-tag requeue]
             (.basicReject this delivery-tag requeue))
    ([this delivery-tag requeue multiple]
       (.basicNack this delivery-tag multiple requeue)))
  (cancel-consumer [this consumer-tag]
    (.basicCancel this consumer-tag))
  (publish
    ([this exchange routing-key body]
       (.basicPublish this exchange routing-key (new AMQP$BasicProperties) body))
    ([this exchange routing-key body options]
       (.basicPublish this exchange routing-key (convert-options-to-basic-properties options) body)))
  (exchange ([this name type]
               (.exchangeDeclare this name (clojure.core/name type)))
    ([this name type options]
       (.exchangeDeclare this name (clojure.core/name type)
                         (if (contains? options :durable)
                           (:durable options)
                           false)
                         (if (contains? options :auto-delete)
                           (:auto-delete options)
                           true)
                         (if (contains? options :internal)
                           (:internal options)
                           false)
                         (if (contains? options :arguments)
                           (:arguments options)
                           {}))))
  (bind-exchange ([this destination source routing-key]
                    (bind-exchange this destination source routing-key {}))
    ([this destination source routing-key arguments]
       (.exchangeBind this destination source routing-key arguments)))
  (unbind-exchange ([this destination source routing-key]
                      (unbind-exchange this destination source routing-key {}))
    ([this destination source routing-key arguments]
       (.exchangeUnbind this destination source routing-key arguments)))
  (delete-exchange ([this exchange]
                      (.exchangeDelete this exchange))
    ([this exchange unused]
       (.exchangeDelete this exchange unused)))
  (flow [this activity]
    (.flow this activity))
  (tx-select [this]
    (.txSelect this))
  (tx-commit [this]
    (.txCommit this))
  (tx-rollback [this]
    (.txRollback this))
  Closable
  (close [this]
    (if (open? this)
      (.close this)))
  ConsumerCreator
  (consumer ([this queue consumer-handler]
               (consumer (.getConnection this) queue consumer-handler))))

(extend-type ShutdownNotifier
  ShutdownNotifyable
  (add-shutdown-notifier [this notifier]
    (.addShutdownListener this (create-shotdown-listener-proxy
                                notifier)))
  Openable
  (open? [this]
    (.isOpen this)))

(defn connect
  "Used to connect to the server.

host - default localhost
username
   default guest
password 
  default guest
port
   default 5672
request-channel-max 
  The maxium number of request for the channel.
request-heartbeat
  The requested heartbeat
virtual-host
ssl - true to enable ssl
"
  [props]
  (let [{:keys [host 
                username 
                password 
                port 
                request-channel-max 
                request-heartbeat 
                virtual-host
                ssl]} props]
    (let [connection (new ConnectionFactory)]
      (doto connection
        (.setHost host)
        (.setUsername (if username
                        username
                        "guest"))
        (.setPassword (if password
                        password
                        "guest"))
        (.setPort (if port
                    port
                    5672)))
      (if ssl
        (.useSslProtocol connection))
      (if virtual-host
        (.setVirtualHost connection virtual-host))
      (if request-channel-max
        (.setRequestedFrameMax connection request-channel-max))
      (if request-heartbeat
        (.setRequestedHeartbeat connection request-heartbeat))
      (.newConnection connection))))
