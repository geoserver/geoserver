GEOSERVER 2.9+ DATA STORE EXTRAS README
---------------------------------------

This package contains an Oracle DataStore implementation that is 
distributed as a separate plug-in.  

https://osgeo-org.atlassian.net/projects/GEOS

Any other issues can be discussed on the mailing list (http://lists.sourceforge.net/lists/listinfo/geoserver-users).

Contains the following data stores:

INSTALLATION

1. Copy gt2-oracle jar in to your GeoServer library directory.  In
   a binary install this is [GEOSERVER_HOME]/server/geoserver/WEB-INF/lib/
   In a war install this is [container]/webapps/geoserver/WEB-INF/lib/

2. Copy the Oracle JDBC driver (ojdbc6.jar or ojdbc7.jar) 
   into [container]/webapps/geoserver/WEB-INF/lib/.
   The driver can be either found in your local Oracle installation, or
   downloaed online from http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html

3. Restart GeoServer.

Note the Oracle jars must be on your classpath for the Oracle datastore
to show up on the menu.  For more information see on needed libraries
and parameters, see: 
http://docs.geoserver.org/latest/en/user/data/database/oracle.html


