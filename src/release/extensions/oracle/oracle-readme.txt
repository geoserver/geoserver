GEOSERVER 2.7+ DATA STORE EXTRAS README
---------------------------------------

This package contains an Oracle DataStore implementation that is 
distributed as a separate plug-in.  It is released just after GeoServer
1.3.3, and contains a bug fix so that connections are not left open.

Please report any bugs with jira (http://jira.codehaus.org/browse/GEOS). 

Any other issues can be discussed on the mailing list (http://lists.sourceforge.net/lists/listinfo/geoserver-users).

Contains the following data stores:

INSTALLATION

1. Copy gt2-oracle jar in to your GeoServer library directory.  In
   a binary install this is [GEOSERVER_HOME]/server/geoserver/WEB-INF/lib/
   In a war install this is [container]/webapps/geoserver/WEB-INF/lib/

2. Restart GeoServer.

Note the Oracle jars must be on your classpath for the Oracle datastore
to show up on the menu.  For more information see on needed libraries
and parameters, see: 
http://docs.geoserver.org/latest/en/user/data/database/oracle.html

COMPATIBILITY

This jar should work with any version of GeoServer based on GeoTools 13.x  
Currently this is anything in 2.7.x.
