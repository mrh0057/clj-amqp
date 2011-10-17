(ns clj-amqp-dsl.connection)

(def create-connection)

(defn set-create-connection-factory
  "Set the function that is used to create connection to your amqp server.  
Must call this function before you do any action with the dsl.

create-connection-function
  The function the returns a new connection to your amqp server."
  [create-connection-function]
  (def create-connection create-connection-function))
