[description]
This module enables scanning of the `$JETTY_BASE/webapps` directory to deploy web applications discovered during the scanning.

[tags]
deployment

[depend]
server
deployer

[lib]
lib/jetty-deploy-${jetty.version}.jar

[files]
webapps/
environments/

[xml]
etc/jetty-deployment-scanner.xml

[ini-template]
#tag::documentation[]
## The web application deploy directory name, or a comma-separated
## list of directories (absolute paths, or relative to $JETTY_BASE).
# jetty.deploy.webappsDir=webapps

## The environments directory name (absolute path, or relative to $JETTY_BASE).
## This is where environment specific configuration files are stored.
# jetty.deploy.environmentsDir=environments

## Monitored directories scan period (seconds).
## This applies to both the webappsDir and environmentsDir.
## The value of 0 (default) disables hot deploy/redeploy/undeploy.
## Positive values enable hot deploy/redeploy/undeploy.
# jetty.deploy.scanInterval=0

## Whether to defer the initial scan at startup.
## Set it to true to have the initial scan deferred until the Server component is started.
## In this way, deploy failures do not fail the Server startup.
## Set it to false (default) to have initial scan occur during Server startup.
# jetty.deploy.deferInitialScan=false
#end::documentation[]
