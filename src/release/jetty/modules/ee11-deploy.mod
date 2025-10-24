[description]
Scans and deploys `ee11` environment web applications.

[tags]
deployment

[environment]
ee11

[before]
ee10-deploy
ee9-deploy
ee8-deploy
core-deploy
static-deploy

[depend]
deployment-scanner
ee11-webapp

[xml]
etc/jetty-ee11-deploy.xml

[ini-template]
# tag::ini-template[]
## Defaults Descriptor for all deployed webapps
# jetty.deploy.defaultsDescriptorPath=${jetty.base}/etc/webdefault-ee11.xml

## Whether to extract *.war files
# jetty.deploy.extractWars=true

## Whether to give the parent classloader priority
# jetty.deploy.parentLoaderPriority=true

## Comma separated list of configuration classes to set.
# jetty.deploy.configurationClasses=

## Pattern to select jars from the container classloader to be scanned (or null to scan no jars)
# jetty.deploy.containerScanJarPattern=.*/jakarta.servlet-api-[^/]*\.jar$|.*wasp-.*\.jar$|.*jakarta.servlet.jsp.jstl-.*\.jar$

## Pattern to select jars from the container classloader to be scanned (or null to scan all jars).
# jetty.deploy.webInfScanJarPattern=

## Pattern to exclude discovered ServletContainerInitializers
# jetty.deploy.servletContainerInitializerExclusionPattern=

## Order of discovered ServletContainerInitializers
# jetty.deploy.servletContainerInitializerOrder=
# end::ini-template[]
