#!/bin/zsh
lein deps && lein install && lein push
cd clj-amqp-dsl
lein deps && lein install && lein push
cd ..
cd clj-rabbitmq
lein deps && lein install && lein push
cd ..
