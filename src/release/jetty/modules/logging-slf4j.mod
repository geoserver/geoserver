# Module: logging-slf4j
# Enables SLF4J logging for Jetty 10.x

[description]
Provides SLF4J logging support for Jetty server startup and operations.
This module adds SLF4J API and implementation JARs to the Jetty startup classpath.

[tags]
logging

[depend]
server

[lib]
lib/slf4j-api-*.jar
lib/jetty-slf4j-impl-*.jar
lib/log4j-api-*.jar
lib/log4j-core-*.jar
lib/log4j-slf4j2-impl-*.jar