(ns clj-amqp.connection)

(defprotocol ConnectionProtocol
  (create-channel [this] [this number]
    "Used to create a channel
number Optional
  Assign the channel a number if available")
  (abort [this] [this options]
    "Used to abort a connection.
timeout
  The numbe for millisecond for the connection to timeout.
close-clode 
  The closing code
closing-message
  The close message
")
  (close [this] [this options]
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
    "The port number"))
