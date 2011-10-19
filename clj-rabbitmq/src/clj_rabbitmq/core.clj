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
            DefaultConsumer
            ShutdownListener
            AMQP]
           com.rabbitmq.client.AMQP$BasicProperties
           [java.util HashMap])
  (:require [clojure.string :as str-utils]))

(defn create-shotdown-listener-proxy [function-handler]
  (proxy [ShutdownListener] []
    (shutdownCompleted [cause]
      (function-handler (make-shutdown-signal-info
                         (.getReason cause)
                         (.getReference cause)
                         (.isHardError cause)
                         (.isInitiatedByApplication cause))))))

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
  (add-shutdown-notifier [this notifier]
    (.addShutdownListener this (create-shotdown-listener-proxy
                                notifier)))
  Closable
  (close [this]
    (if (open? this)
      (.close this)))
  Openable
  (open? [this]
    (.isOpen this)))

(defn- create-consumer-proxy [channel consumer]
  (proxy [DefaultConsumer] [channel]
    (handleCancel [consumer-tag])
    (handleCancelOk [consumer-tag])
    (handleConsumeOk [consumer-tag])
    (handleDelivery [consumer-tag ^com.rabbitmq.client.Envelope envelope properties ^bytes body]
      (consumer body
                (make-envelope (.getDeliveryTag envelope)
                               (.getExchange envelope)
                               (.getRoutingKey envelope)
                               (.isRedeliver envelope))
                (convert-message-properites properties)))
    (handleRecoverOk [])
    (handleShutdownSignal [consumerTag sig])))

(extend-type Channel
  ChannelProtocol
  (queue-exists? [this queue]
    (try
      (.queueDeclarePassive this queue)
      (catch Exception e
        false)))
  (exclusive-queue [this]
    (.getQueue (.queueDeclare this)))
  (bind-queue [this queue exchange routing-key]
    (.queueBind this queue exchange routing-key))
  (unbind-queue [this queue exchange routing-key]
    (.queueUnbind this queue exchange routing-key))
  (declare-queue [this queue durable exclusive auto-delete]
    (.getQueue (.queueDeclare this queue durable exclusive auto-delete (new HashMap))))
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
  (consumer ([this queue consumer]
               (.basicConsume this queue false (create-consumer-proxy this consumer)))
    ([this queue consumer options]))
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
                         (new HashMap))))
  (bind-exchange [this destination source routing-key]
    (.exchangeBind this destination source routing-key))
  (unbind-exchange [this destination source routing-key]
    (.exchangeUnbind this destination source routing-key))
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
