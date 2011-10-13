(ns clj-rabbitmq.internal.core-test
  (:use clojure.test
        clj-rabbitmq.internal.core)
  (:import com.rabbitmq.client.AMQP$BasicProperties
           [java.util Date]))

(deftest convert-message-properites-test
  (let [basic-properties (new AMQP$BasicProperties
                              "content-type"
                              "encoding"
                              {:head "ers"}
                              1
                              2
                              "3"
                              "me"
                              "sometime"
                              "4"
                              (new Date)
                              "type"
                              "id"
                              "5"
                              "10")
        converted (convert-message-properites basic-properties)]
    (is (= (:content-type converted) "content-type"))
    (is (= (:content-encoding converted) "encoding"))
    (is (= (:headers converted) {:head "ers"}))
    (is (= (:delivery-mode converted) 1))
    (is (= (:priority converted) 2))
    (is (= (:correlation-id converted) "3"))
    (is (= (:reply-to converted) "me"))
    (is (= (:expiration converted) "sometime"))
    (is (= (:message-id converted) "4"))
    (is (:timestamp converted))
    (is (= (:type converted) "type"))
    (is (= (:user-id converted) "id"))
    (is (= (:app-id converted) "5"))
    (is (= (:cluster-id converted) "10"))))

(deftest convert-options-to-basic-properties-test
  (let [options {:content-type "content-type"
                 :content-encoding "encoding"
                 :headers {:head "me"}
                 :delivery-mode :persistent
                 :priority 1
                 :correlation-id "3"
                 :reply-to "me"
                 :expiration "now"
                 :message-id "4"
                 :timestamp (new Date)
                 :type "ty"
                 :user-id "5"
                 :app-id "6"
                 :cluster-id "7"}
        props (convert-options-to-basic-properties options)]
    (is (= (.getContentType props) "content-type"))
    (is (= (.getContentEncoding props) "encoding"))
    (is (= (.getHeaders props) {:head "me"}))
    (is (= (.getDeliveryMode props) 2))
    (is (= (.getPriority props) 1))
    (is (= (.getCorrelationId props) "3"))
    (is (= (.getReplyTo props) "me"))
    (is (= (.getExpiration props) "now"))
    (is (= (.getMessageId props) "4"))
    (is (.getTimestamp props))
    (is (= (.getType props) "ty"))
    (is (= (.getUserId props) "5"))
    (is (= (.getAppId props) "6"))
    (is (= (.getClusterId props) "7"))))
