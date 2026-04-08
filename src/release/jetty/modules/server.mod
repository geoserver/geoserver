[description]
Enables and configures the Jetty server.
This module does not enable any network protocol support.
To enable a specific network protocol such as HTTP/1.1, you must enable the correspondent Jetty module.

[after]
jvm
ext
resources

[depend]
threadpool
scheduler
bytebufferpool
http-config

[lib]
lib/jetty-http-${jetty.version}.jar
lib/jetty-server-${jetty.version}.jar
lib/jetty-xml-${jetty.version}.jar
lib/jetty-util-${jetty.version}.jar
lib/jetty-io-${jetty.version}.jar

[xml]
etc/jetty.xml

[jpms]
# ALL-DEFAULT is necessary to expose JDK modules such as java.sql
# or java.instrument to the ModuleLayer of an eeN environment.
# ALL-MODULE-PATH is necessary to allow the org.eclipse.jetty.xml
# JPMS module to load classes from other JPMS modules such as
# org.eclipse.jetty.io while processing XML files.
add-modules: ALL-DEFAULT,ALL-MODULE-PATH

[ini-template]
# tag::documentation[]
### Server configuration
## Whether ctrl+c on the console gracefully stops the Jetty server.
# jetty.server.stopAtShutdown=true

## Dump the state of the Jetty server, components, and webapps after startup.
# jetty.server.dumpAfterStart=false

## The temporary directory used by the Jetty server and as a root for its contexts.
# jetty.server.tempDirectory=

## Dump the state of the Jetty server, components, and webapps before shutdown.
# jetty.server.dumpBeforeStop=false

## Whether the handlers of the ContextHandlerCollection can be updated once the server is started.
## If set to false, then <env>-deploy module jetty.deploy.scanInterval should also be set to 0.
# jetty.server.contexts.dynamic=true

## Should the DefaultHandler serve the jetty favicon.ico from the root.
# jetty.server.default.serveFavIcon=true

## Should the DefaultHandler show a list of known contexts in a root 404 response.
# jetty.server.default.showContexts=true
# end::documentation[]
