(ns
    ^{:doc "All functions are expected to be wrap in with-channel."}
   clj-amqp.core
  (:require [clj-amqp.channel :as channel]))

(def
  ^:dynamic *channel*)

(defmacro with-channel
  "Used to start a block of code that binds to a specific channel.

channel
  The channel to bind to.
body
  The expressions to execute."
  [channel & body]
  `(binding [clj-amqp.core/*channel* ~channel]
     ~@body))

(defn acknowledge
  "Used to acknowledge the delivery of a message.

delivery-tag 
  The delivery tag to acknowledge"[delivery-tag]
  (channel/acknowledge *channel* delivery-tag))

(defn reject
  "Used to not acknowledge a message delivery.

delivery-tag 
  The delivery tag to not acknowledge.
requeue
  To requeue the messae
multiple
  reject multiple"
  ([delivery-tag requeue]
     (reject delivery-tag requeue false))
  ([delivery-tag requeue multiple]
     (channel/reject *channel* delivery-tag requeue multiple)))

(defn queue-exists?
  "Checks to see if a queue exists.

queue
  The name of the queue
returns
  true if the queue exists"
  [queue]
  (channel/queue-exists? *channel* queue))

(defn add-shutdown-notifier
  "Used to notify a function in the event the connection closes.

notifier
  The function to call with the channel is closed.
    Takes 1 parameter with is the ShutdownSignalInfo"
  [notifier]
  (add-shutdown-notifier *channel* notifier))

(defn open? []
  "Checks to see if the channel is open."
  (open? *channel*))

(defn exclusive-queue
  "Creates a server-named exclusive, autodelete, non-durable queue.

return
  The name of the queue."
  []
  (channel/exclusive-queue *channel*))

(defn bind-queue
  "Used to create a queue.

queue
  The name of the queue
exchange
  The name of the exchange
routing-key
  The routing key"
  [queue exchange routing-key]
  (channel/bind-queue *channel* queue exchange routing-key))

(defn unbind-queue
  "Used to unbind a queue
queue
  The name of the queue
exchange
  The name of the exchange
routing-key
  The routing key of the exchange."
  [queue exchange routing-key]
  (channel/unbind-queue *channel* queue exchange routing-key))

(defn declare-queue
  "Used to create a queue

queue
  The name of the queue to create
durable
  If the queue serivces after the server is shutdown.
auto-delete
  To automatically delete the queue"
  [queue durable exclusive auto-delete]
  (channel/declare-queue *channel* queue durable exclusive auto-delete))

(defn purge-queue
  "Removes all of the messages from the queue.

queue
  The queue to remove the messages from."
  [queue]
  (channel/purge-queue *channel* queue))

(defn delete-queue
  "Deletes a queue for the server.

queue
  The name of the queue to delete
options
  :unused
    true to delete the queue if its currently not in use.
  :empty
    true to delete teh queue if its not empty"
  [queue & options]
  (if (empty? options)
    (channel/delete-queue *channel* queue)
    (channel/delete-queue *channel* queue (apply hash-map options))))

(defn consumer
  "Used to create a consumer
queue
  The name of the queue to consume
consumer
  A function that consumes the incoming messages.
  The function takes three arguments the body, Envelope, and properties.

returns
  The consumer tag."
  [queue consumer]
  (channel/consumer *channel* queue consumer))

(defn cancel-consumer
  "Cancels a consumer

consumer-tag
  The tag of the consumer to cancel."
  [consumer-tag]
  (channel/cancel-consumer *channel* consumer-tag))

(defn publish
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
  :mandatory defaults to false. True to make the publish mandatory.
  :immediate defaults to false. True to request to be immediately published"
  [exchange routing-key body & options]
  (if (empty? options)
    (channel/publish *channel* exchange routing-key body)
    (channel/publish *channel* exchange routing-key body (apply hash-map options))))

(defn exchange
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
   The exchange is internal and can't be directly published to by the client."
  [name type & options]
  (if (empty? options)
    (channel/exchange *channel* name type)
    (channel/exchange *channel* name type (apply hash-map options))))

(defn bind-exchange
  "Used to bind an exchange to another exchange.

destination
  The exchange that contains the messages.
source
  The exchange that receives the messages.
routing-key
  The routing key to use to the bind the exchange to"
  [destination source routing-key]
  (channel/bind-exchange *channel* destination source routing-key))

(defn unbind-exchange
  "Used unbind an exchange to another exchange.

destination
  The exchange that contains the messages.
source
  The exchange that receives the messages.
routing-key
  The routing key to use to the bind the exchange to"
  [destination source routing-key]
  (channel/unbind-exchange *channel* destination source routing-key))

(defn delete-exchange
  "Used to delete the exchange from the server.

exchange
  The exchange to delete
unused
  delete the exchange if its unused."
  ([exchange]
     (channel/delete-exchange *channel* exchange))
  ([exchange unused]
     (channel/delete-exchange *channel* exchange unused)))

(defn flow
  "Used to start/stop the flow of messages.

activity
  True to start sending messages.
  False to stop stop sending messages."
  [active]
  (channel/flow *channel* active))

(defn tx-select
  "Enables transactions on the channel"
  []
  (channel/tx-select *channel*))

(defn tx-commit
  "Commits the current transaction."
  []
  (channel/tx-commit *channel*))

(defn tx-rollback
  "Rollbacks the current transaction on the channel."
  []
  (channel/tx-rollback *channel*))
