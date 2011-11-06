(defproject clj-amqp-dsl "0.1.0-SNAPSHOT"
  :description "A dsl that interacts with an amqp server."
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [clj-amqp "0.1.0-SNAPSHOT"]
                 [commons-pool/commons-pool "1.5.6"]]
  :dev-dependencies [[swank-clojure "1.3.1"]
                     [clj-rabbitmq "0.1.0-SNAPSHOT"]
                     [clj-bson "0.1.0-SNAPSHOT"]
                     [lein-marginalia "0.6.0"]
                     [marginalia "0.7.0-SNAPSHOT"]
                     [midje "1.2.0" :exclusions [org.clojure.contrib/core]]]
  :warn-on-reflection true
  :aot [clj-amqp-dsl.internal.ThreadFactory])
