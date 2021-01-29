#
# Jetty HTTP Connector
#
[description]
Enables an HTTP connector on the server.
By default HTTP/1 is support, but HTTP2C can
be added to the connector with the http2c module.

[tags]
connector
http

[depend]
server

[xml]
etc/jetty-http.xml

[ini-template]
### HTTP Connector Configuration

## Connector host/address to bind to
# jetty.http.host=0.0.0.0

## Connector port to listen on
jetty.http.port=8080

## Connector idle timeout in milliseconds
jetty.http.idleTimeout=30000

## Number of acceptors (-1 picks default based on number of cores)
# jetty.http.acceptors=-1

## Number of selectors (-1 picks default based on number of cores)
# jetty.http.selectors=-1

## ServerSocketChannel backlog (0 picks platform default)
# jetty.http.acceptQueueSize=0

## Thread priority delta to give to acceptor threads
# jetty.http.acceptorPriorityDelta=0

## The requested maximum length of the queue of incoming connections.
# jetty.http.acceptQueueSize=0

## Enable/disable the SO_REUSEADDR socket option.
# jetty.http.reuseAddress=true

## Enable/disable TCP_NODELAY on accepted sockets.
# jetty.http.acceptedTcpNoDelay=true

## The SO_RCVBUF option to set on accepted sockets. A value of -1 indicates that it is left to its default value.
# jetty.http.acceptedReceiveBufferSize=-1

## The SO_SNDBUF option to set on accepted sockets. A value of -1 indicates that it is left to its default value.
# jetty.http.acceptedSendBufferSize=-1

## Connect Timeout in milliseconds
# jetty.http.connectTimeout=15000

## HTTP Compliance: RFC7230, RFC7230_LEGACY, RFC2616, RFC2616_LEGACY, LEGACY or CUSTOMn
# jetty.http.compliance=RFC7230_LEGACY
