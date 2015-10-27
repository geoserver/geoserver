#
# Deploy Feature
#

[depend]
webapp

[lib]
lib/jetty-deploy-${jetty.version}.jar

[files]
webapps/

[xml]
etc/jetty-deploy.xml

[ini-template]
## DeployManager configuration
# Monitored Directory name (relative to jetty.base)
# jetty.deploy.monitoredDirName=webapps

