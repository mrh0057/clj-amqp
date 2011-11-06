#!/bin/zsh

lein clean && lein deps && lein install
cd clj-amqp-dsl
lein clean && lein deps && lein install
cd ..
cd clj-rabbitmq
lein clean && lein deps && lein install
cd ..
