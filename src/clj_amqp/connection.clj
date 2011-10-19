(ns clj-amqp.connection)

(defrecord ShutdownSignalInfo [reason
                               reference
                               hard-error
                               initiated-by-application])

(defn make-shutdown-signal-info [reason reference hard-error initiated-by-application]
  (ShutdownSignalInfo. reason reference hard-error initiated-by-application))

(defprotocol ConnectionProtocol
  (create-channel [this] [this number]
    "Used to create a channel
number Optional
  Assign the channel a number if available")
  (close-with-timeout [this timeout]
    "Use to close the connection.
timeout
  The number of millisecond for the connection to timout.")
  (address [this])
  (channel-max [this]
    "Get the maxium number channel.")
  (frame-max [this]
    "The maximum frame size.")
  (heart-beat [this]
    "Get the negotiated heartbeat interval")
  (port [this]
    "The port number")
  (add-shutdown-notifier [this notifier]
    "Adds a shutdown notifier for the connection.

notifier
  The function to be notified when the conenction is terminated.
    Function takes on parameter ShutdownSignalInfo."))
