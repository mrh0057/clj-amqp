# clj-ampq

A common low level api for communicating with amqp servers.  Provides an implemenation that use rabbitmq library to provide the connection to the amqp server.

## Usage

Goal is to have a low level library that people can then create DSL specific for their application.

Core contains a basic low level dsl where you can do things like:

  (with-channel my-channel
    (acknowledge delivery))

When you do that you bind the channel to a thread local variable called *channel* that exists in clj-amqp.core.

## License

Copyright (C) 2011 Matt Hoyt

Distributed under the Eclipse Public License, the same as Clojure.
