(ns clj-amqp.channel)

(defprotocol Channel
  (exclusive-queue [this]
    "Creates a server-named exclusive, autodelete, non-durable queue.

return
  The name of the queue.")
  (acknowledge [this delivery-tag])
  (cancel-consumer [this consumer-tag]
    "Cancels a consumer

consumer-tag
  The tag of the consumer to cancel.")
  (consumer [this queue consumer & options]
    "Used to create a consumer
queue
  The name of the queue to consume
consumer
  A function that consumes the incoming messages.
  The function takes
options
  :auto-acknowledge defaults to false
  :consumer-tag defaults is generated
  :no-local defaults to true
  :exclusive defaults to false")
  (publish [this exchange routing-key body & options]
    "Used to publish a message.

exchange
  The exchange to publish the message on.
routing-key
  The routing key to
body
  The body of the message to publish.
options
  :mandatory defaults to false. True to make the publish mandatory.
  :immediate defaults to false. True to request to be immediately published")
  (exchange [this name type & options]
    "Used to create an exchange.

name 
  The name of the exchange to create
type
  The type of exchange.
  :direct 
    Send messages to each queue that matches the routing key exactly.
  :fan-out 
    Send a message to all of the queues.
  :topic 
    Must have a list of words delimited by a . The maxium length is limitted to 255 bytes.
    For more information: http://www.rabbitmq.com/tutorials/tutorial-five-python.html
options
  :durable 
   The exchange survives a server restart.  Defaults to false
  :auto-delete
   The exchange is automattically deleted. Defaults to false"))
