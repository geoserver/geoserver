GEOSERVER ORACLE DATA STORE EXTRAS README
-----------------------------------------

This package contains an Oracle DataStore implementation that is 
distributed as a separate plug-in.  

Please report any bugs in jira (https://osgeo-org.atlassian.net/projects/GEOS).

Any other issues can be discussed on the mailing list (https://sourceforge.net/projects/geoserver/lists/geoserver-users).

Contains the following data stores:

 - Oracle NG
 - Oracle NG (JNDI)

INSTALLATION

1. Copy the gt-jdbc-oracle and ojdbc8 jars into your GeoServer library directory.
   In a binary install this is [GEOSERVER_HOME]/server/geoserver/WEB-INF/lib/
   In a war install this is [container]/webapps/geoserver/WEB-INF/lib/

2. Restart GeoServer.

Note the Oracle jars must be on your classpath for the Oracle datastore
to show up on the menu. For more information see on needed libraries
and parameters, see: 
https://docs.geoserver.org/latest/en/user/data/database/oracle.html
