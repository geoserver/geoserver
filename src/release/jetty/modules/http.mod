#
# Jetty HTTP Connector
#

[depend]
server

[xml]
etc/jetty-http.xml

[ini-template]
### HTTP Connector Configuration

## HTTP port to listen on
jetty.port=8080

## HTTP idle timeout in milliseconds
http.timeout=30000

## HTTP Socket.soLingerTime in seconds. (-1 to disable)
# http.soLingerTime=-1

## Parameters to control the number and priority of acceptors and selectors
# http.selectors=1
# http.acceptors=1
# http.selectorPriorityDelta=0
# http.acceptorPriorityDelta=0
