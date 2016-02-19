#
# Base Server Module
#

[optional]
jvm
ext
resources

[lib]
# servlet-api and jetty-schemas modified to match versions/artifact ids from maven
lib/javax.servlet-api-3.1.0.jar
lib/jetty-schemas-3.1.M0.jar
lib/jetty-http-${jetty.version}.jar
lib/jetty-server-${jetty.version}.jar
lib/jetty-xml-${jetty.version}.jar
lib/jetty-util-${jetty.version}.jar
lib/jetty-io-${jetty.version}.jar

[xml]
etc/jetty.xml

[ini-template]
##
## Server Threading Configuration
##
# minimum number of threads
threads.min=10
# maximum number of threads
threads.max=200
# thread idle timeout in milliseconds
threads.timeout=60000
# buffer size for output
jetty.output.buffer.size=32768
# request header buffer size
jetty.request.header.size=8192
# response header buffer size
jetty.response.header.size=8192
# should jetty send the server version header?
jetty.send.server.version=true
# should jetty send the date header?
jetty.send.date.header=false
# What host to listen on (leave commented to listen on all interfaces)
#jetty.host=myhost.com
# Dump the state of the Jetty server, components, and webapps after startup
jetty.dump.start=false
# Dump the state of the Jetty server, before stop
jetty.dump.stop=false
# Enable delayed dispatch optimisation
jetty.delayDispatchUntilContent=false
