(ns clj-rabbitmq.core
  (:use clj-rabbitmq.internal.options
        clj-rabbitmq.internal.constants
        clj-rabbitmq.internal.core
        clj-amqp.connection
        clj-amqp.channel)
  (:import [com.rabbitmq.client ConnectionFactory
            Connection
            Channel
            DefaultConsumer
            AMQP]
           com.rabbitmq.client.AMQP$BasicProperties
           [java.util HashMap])
  (:require [clojure.string :as str-utils]))

(extend-type Connection
  ConnectionProtocol
  (create-channel
    ([this]
       (.createChannel this))
    ([this number]
       (.createChannel this number)))
  (address [this]
    (.getAddress this))
  (close
    ([this]
       (.close this))
    ([this timeout]
       (.close this timeout)))
  (channel-max [this]
    (.getChannelMax this))
  (frame-max [this]
    (.getFrameMax this))
  (heart-beat [this]
    (.getHeartbeat this))
  (port [this]
    (.getPort this)))

(defn- create-consumer-proxy [channel consumer]
  (proxy [DefaultConsumer] [channel]
    (handleCancel [consumer-tag])
    (handleCancelOk [consumer-tag])
    (handleConsumeOk [consumer-tag])
    (handleDelivery [consumer-tag ^com.rabbitmq.client.Envelope envelope properties body]
      (consumer body
                (make-envelope (.getDeliveryTag envelope)
                               (.getExchange envelope)
                               (.getRoutingKey envelope)
                               (.isRedeliver envelope))
                (convert-message-properites properties)))
    (handleRecoverOk [])
    (handleShutdownSignal [consumerTag sig])))

(defn- keyword-to-string [keyword]
  (-> (str keyword)
      rest
      str-utils/join))

(extend-type Channel
  ChannelProtocol
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
               (.exchangeDeclare this name (keyword-to-string type)))
    ([this name type options]
       (.exchangeDeclare this name (keyword-to-string type)
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
    (.txRollback this)))

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
  (let [{host :host
         username :username
         password :password
         port :port
         request-channel-max :request-channel-max
         request-heartbeat :request-heartbeat
         virtual-host :virtual-host
         ssl :ssl} props]
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
