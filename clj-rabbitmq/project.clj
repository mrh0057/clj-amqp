(defproject clj-rabbitmq "0.1.0-SNAPSHOT"
  :description "FIXME: write"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [com.rabbitmq/amqp-client "2.6.1"]]
  :dev-dependencies [[swank-clojure "1.3.1"]
                     [lein-midje "1.0.3"]
                     [midje "1.1.1" :exclusions [org.clojure/clojure
                                                 org.clojure.contrib/core]]])
