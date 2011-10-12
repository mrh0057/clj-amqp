(ns clj-rabbitmq.core
    (:use clj-rabbitmq.internal.options)
  (:import [com.rabbitmq.client ConnectionFactory
            Connection
            AMQP]))

(extend-type Connection
  ConnectionProtocol
  (create-channel
    ([this]
       (.createChannel this))
    ([this timeout]
       (.abort this)))
  )

(defn connect
  "Used to connect to the server.

host - default localhost
username
   default guest
password 
  default guest
port
   default 5621
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
        (.setRequestedFrameMax request-channel-max))
      (if request-heartbeat
        (.setRequestedHeartbeat request-heartbeat))
      (.newConnection connection))))
