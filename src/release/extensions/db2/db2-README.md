DB2 DATA STORE EXTENSION
------------------------

This package contains a DB2 DataStore implementation that is
distributed as a separate plug-in.

Please report any bugs with jira (https://osgeo-org.atlassian.net/projects/GEOS).

Any other issues can be discussed on the mailing list (http://lists.sourceforge.net/lists/listinfo/geoserver-users).

INSTALLATION

1. Copy the included jar files to your
GeoServer library directory. In a binary install this is at
[GEOSERVER_HOME]/server/geoserver/WEB-INF/lib/
In a war install this is [container]/webapps/geoserver/WEB-INF/lib/

3. Restart GeoServer.

DB2 should now show up as an option in the web admin tool at
Config -> Data -> DataStores -> New. Fill out the appropriate params.
For more information http://docs.geoserver.org/latest/en/user/data/database/db2.html

COMPATIBILITY

This jar should work with any version of GeoServer based on GeoTools 15.x and above
This is GeoServer versions 2.9.x and above.
