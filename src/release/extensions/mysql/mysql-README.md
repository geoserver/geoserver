MySQL Extension README

This package contains a MySQL DataStore implementation that is 
distributed as a separate plug-in.  This plug-in is still experimental,
if you have feedback please let us know.  See 
http://docs.geoserver.org/latest/en/user/data/database/sqlserver.html for more info

Please report any bugs with jira (https://osgeo-org.atlassian.net/projects/GEOS).

Any other issues can be discussed on the mailing list (http://lists.sourceforge.net/lists/listinfo/geoserver-users).

Contains the following data stores:

INSTALLATION

1. Copy included gt2-jdbc-mysql and mysql-connector-java jars included to your 
   GeoServer library directory.  In a binary install this is at 
   [GEOSERVER_HOME]/server/geoserver/WEB-INF/lib/
   In a war install this is [container]/webapps/geoserver/WEB-INF/lib/

2. Restart GeoServer.

MySQL should now show up as an option in the web admin tool at 
Config -> Data -> DataStores -> New.  Fill out the appropriate params.

COMPATIBILITY

This jar should work with any version of GeoServer based on GeoTools 15.x  
Currently this is anything in 2.9.x.
