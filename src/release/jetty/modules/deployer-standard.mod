[description]
This module enables the standard Deployer, which adds and starts ContextHandler
instances produced by the deployment-scanner module.

[tags]
deployment

[provides]
deployer|default

[depends]
server

[lib]
lib/jetty-deploy-${jetty.version}.jar

[xml]
etc/jetty-deployer-standard.xml

[ini-template]

## Whether during redeploy the new ContextHandler instance is started before the old ContextHandler is
## replaced and stopped, otherwise it is started after the old ContextHandler is replaced and stopped.
# jetty.deploy.atomicRedeploy=true
