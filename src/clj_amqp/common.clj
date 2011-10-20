(ns clj-amqp.common)

(defprotocol Openable
  (open? [this]
    "Checks to see if something is open."))

(defprotocol Closable
  (close [this] "Closing whatever this is.

if this is already closed it should do nothing."))

(defrecord ShutdownSignalInfo [exception
                               reason
                               reference
                               hard-error
                               initiated-by-application])

(defn make-shutdown-signal-info [exception reason reference hard-error initiated-by-application]
  (ShutdownSignalInfo. exception reason reference hard-error initiated-by-application))

(defprotocol ShutdownNotifyable
  (add-shutdown-notifier [this notifier]
    "Used to notify a function in the event the connection closes.

notifier
  The function to call with the connection is closed.
    Takes 1 parameter with is the ShutdownSignalInfo"))


(defprotocol ConsumerCreator
  (consumer [this queue consumer]
    "Used to create a consumer on its on channel.
queue
  The name of the queue to consume
consumer
  A function that consumes the incoming messages.
  The function takes three arguments the body, Envelope, and properties.

returns
  The consumer tag."))
