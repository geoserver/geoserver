#
# Jetty HTTPS Connector
#
[description]
Adds HTTPS protocol support to the TLS(SSL) Connector

[tags]
connector
https
http
ssl

[depend]
ssl

[optional]
http2
http-forwarded

[xml]
etc/jetty-https.xml

[ini-template]
## HTTPS Configuration
# HTTP port to listen on
jetty.ssl.port=8443
# HTTPS idle timeout in milliseconds
jetty.ssl.idleTimeout=30000
