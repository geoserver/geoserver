#
# Jetty HTTPS Connector
#

[depend]
ssl

[xml]
etc/jetty-https.xml

[ini-template]
## HTTPS Configuration
# HTTP port to listen on
https.port=8443
# HTTPS idle timeout in milliseconds
https.timeout=30000
# HTTPS Socket.soLingerTime in seconds. (-1 to disable)
# https.soLingerTime=-1

