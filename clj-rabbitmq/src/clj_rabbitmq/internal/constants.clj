(ns clj-rabbitmq.internal.constants
  (:import [com.rabbitmq.client AMQP]))

(def *delivery-no-persistent* 1)

(def *delivery-persistent* 2)

(def *access-refused* AMQP/ACCESS_REFUSED)

(def *channel-error* AMQP/CHANNEL_ERROR)

(def *command-invalid* AMQP/COMMAND_INVALID)

(def *connection-forced* AMQP/CONNECTION_FORCED)

(def *content-to-large* AMQP/CONTENT_TOO_LARGE)

(def *frame-body* AMQP/FRAME_BODY)

(def *frame-end* AMQP/FRAME_END)

(def *frame-error* AMQP/FRAME_ERROR)

(def *frame-header* AMQP/FRAME_HEADER)

(def *frame-heartbeat* AMQP/FRAME_HEARTBEAT)

(def *frame-method* AMQP/FRAME_METHOD)

(def *frame-min-size* AMQP/FRAME_MIN_SIZE)

(def *internal-error* AMQP/INTERNAL_ERROR)

(def *invalid-path* AMQP/INVALID_PATH)

(def *no-consumers* AMQP/NO_CONSUMERS)

(def *no-route* AMQP/NO_ROUTE)

(def *not-allowed* AMQP/NOT_ALLOWED)

(def *not-found* AMQP/NOT_FOUND)

(def *not-implemented* AMQP/NOT_IMPLEMENTED)

(def *precondition-failed* AMQP/PRECONDITION_FAILED)

(def *reply-success* AMQP/REPLY_SUCCESS)

(def *resource-locked* AMQP/RESOURCE_LOCKED)

(def *syntax-error* AMQP/SYNTAX_ERROR)

(def *unexpected-frame* AMQP/UNEXPECTED_FRAME)
