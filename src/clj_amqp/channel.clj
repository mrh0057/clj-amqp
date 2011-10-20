(ns clj-amqp.channel
  (:use clj-amqp.common))

(defrecord Envelope [delivery-tag
                     exchange
                     routing-key
                     redelivered])

(defrecord MessageProperties [class-id
                              content-type
                              content-encoding
                              headers
                              delivery-mode
                              priority
                              correlation-id
                              reply-to
                              expiration
                              message-id
                              timestamp
                              type
                              user-id
                              app-id
                              cluster-id])

(defn make-message-properties [class-id
                               content-type
                               content-encoding
                               headers
                               delivery-mode
                               priority
                               correlation-id
                               reply-to
                               expiration
                               message-id
                               timestamp
                               type
                               user-id
                               app-id
                               cluster-id]
  (MessageProperties. class-id
                      content-type
                      content-encoding
                      headers
                      delivery-mode
                      priority
                      correlation-id
                      reply-to
                      expiration
                      message-id
                      timestamp
                      type
                      user-id
                      app-id
                      cluster-id))

(defn make-envelope [delivery-tag exchange routing-key redelivered]
  (Envelope. delivery-tag
             exchange
             routing-key
             redelivered))

(defrecord ConsumerInfo [consumer-tag channel])

(defn make-consumer-info [consumer-tag channel]
  (ConsumerInfo. consumer-tag channel))

(extend-type ConsumerInfo
  ShutdownNotifyable
  (add-shutdown-notifier [this notifier]
    (add-shutdown-notifier (:channel this) notifier)))

(defprotocol ChannelProtocol
  (queue-exists? [this queue]
    "Checks to see if a queue exists.
queue
  The name of the queue.
returns
  true if the queue exists")
  (exclusive-queue [this]
    "Creates a server-named exclusive, autodelete, non-durable queue.

return
  The name of the queue.")
  (bind-queue [this queue exchange routing-key]
    "Used to create a queue.

queue
  The name of the queue
exchange
  The name of the exchange
routing-key
  The routing key")
  (unbind-queue [this queue exchange routing-key]
    "Used to unbind a queue
queue
  The name of the queue
exchange
  The name of the exchange
routing-key
  The routing key of the exchange.")
  (declare-queue [this queue durable exclusive auto-delete]
    "Used to create a queue

queue
  The name of the queue to create
durable
  If the queue serivces after the server is shutdown.
auto-delete
  To automatically delete the queue")
  (delete-queue [this queue] [this queue options]
    "Deletes a queue for the server.

queue
  The name of the queue to delete
options
  :unused
    true to delete the queue if its currently not in use.
  :empty
    true to delete teh queue if its not empty")
  (purge-queue [this queue]
    "Removes all of the messages from the queue.

queue
  The queue to remove the messages from.")
  (acknowledge [this delivery-tag]
    "Used to acknowledge when a message is received.

delivery-tag
  The tag to acknowledge the message.")
  (reject [this delivery-tag requeue] [this delivery-tag requeue multiple]
    "Rejects a message.

deliver-tag
  The delivery tag
requeue
  Requeue the message
multiple 
  true reject all messages up to and including the supplied delivery tag.")
  (cancel-consumer [this consumer-tag]
    "Cancels a consumer

consumer-tag
  The tag of the consumer to cancel.")
  (publish [this exchange routing-key body] [this exchange routing-key body options]
    "Used to publish a message.

exchange
  The exchange to publish the message on.
routing-key
  The routing key to
body
  The body of the message to publish.
options
  :content-type 
    defaults to nil
  :content-encoding d
    efaults to nil
  :headers 
    defaults to nil
  :delivery-mode 
    defaults to nil
   :persistent  The message survives a restart.
   :nonpersitent  The message maybe lost if the server restarts.
  :priority 
     defaults to nil
  :correlation-id 
     defaults to nil
  :reply-to 
     defaults to nil
  :expiration 
    defaults to nil
  :message-id 
    defaults to nil
  :timestamp
    defaults to nil
  :type
    defaults to nil
  :user-id
  :app-id
    defaults to nil
  :cluster-id
    defaults to nil
  :mandatory defaults to false. 
    True to make the publish mandatory.
  :immediate defaults to false. 
    True to request to be immediately published")
  (exchange [this name type] [this name type options]
    "Used to create an exchange.

name 
  The name of the exchange to create
type
  The type of exchange.
  :direct 
    Send messages to each queue that matches the routing key exactly.
  :fanout 
    Send a message to all of the queues.
  :topic 
    Must have a list of words delimited by a . The maxium length is limitted to 255 bytes.
    For more information: http://www.rabbitmq.com/tutorials/tutorial-five-python.html
options
  :durable 
   The exchange survives a server restart.  Defaults to false
  :auto-delete
   The exchange is automattically deleted. Defaults to false
  :internal
   The exchange is internal and can't be directly published to by the client.")
  (bind-exchange [this destination source routing-key]
    "Used to bind an exchange to another exchange.

destination
  The exchange that contains the messages.
source
  The exchange that receives the messages.
routing-key
  The routing key to use to the bind the exchange to")
  (unbind-exchange [this destination source routing-key]
    "Used unbind an exchange to another exchange.

destination
  The exchange that contains the messages.
source
  The exchange that receives the messages.
routing-key
  The routing key to use to the bind the exchange to")
  (delete-exchange [this exchange] [this exchange unused]
    "Used to delete the exchange from the server.

exchange
  The exchange to delete
unused
  delete the exchange if its unused.")
  (flow [this activty]
    "Used to start/stop the flow of messages.

activity
  True to start sending messages.
  False to stop stop sending messages.")
  (tx-select [this]
    "Enables transactions on the channel")
  (tx-commit [this]
    "Commits the current transaction.")
  (tx-rollback [this]
    "Rollbacks the current transaction on the channel."))
