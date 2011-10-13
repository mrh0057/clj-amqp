(ns clj-amqp-json.core
  (:use clj-json.core)
  (:require [clj-amqp.core :as amqp]))

(defn encode-body
  "Will not encode dates.  If you are sending dates convert to long."
  [body]
  (.getBytes (generate-string body)))

(defn decode-body
  "Decodes a body of a message if its json."
  ([body]
     (parse-string (new String body) true))
  ([body char-set]
     (parse-string (new String body char-set) true)))

(defmacro publish
  "Used to publish a message thats encode with json"
  [exchange routing-key body & options]
  `(clj-amqp.core/publish ~exchange ~routing-key
                          (clj-amqp.core/encode-body ~body) ~@options))

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
                    (clj-amqp-json.core/decode-body body# (:content-type ~'properties))
                    (clj-amqp-json.core/decode-body body#))]
       ~@body)))
