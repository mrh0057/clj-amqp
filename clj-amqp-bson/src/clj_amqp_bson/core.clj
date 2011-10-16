(ns clj-amqp-bson.core
  (:use clj-bson.core))

(defmacro publish
  "Used to publish a message thats encode with json"
  [exchange routing-key body & options]
  `(clj-amqp.core/publish ~exchange ~routing-key
                          (clj-bson.core/encode ~body) ~@options))

(defmacro create-consumer
  "Used to create a consumer function.

body 
  The body of the expression to execute.

binds 
  body 
    The decoded json message
  envelope
    The envlope
  properties
    The properteis for the message."
  [& body]
  `(fn [body# ~'envelope ~'properties]
     (let [~'body (if (:content-type ~'properties)
                    (clj-bson.core/decode body# (:content-type ~'properties))
                    (clj-bson.core/decode body#))]
       ~@body)))
