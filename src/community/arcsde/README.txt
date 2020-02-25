GeoServer ArcSDE Datastore Extension
====================================

This extension adds functionality to GeoServer to allow connection to ArcSDE
instances.

IMPORTANT NOTE:

Due to licensing issues, there are certain files that are not distributed
with this archive that are REQUIRED for it to work!

Files required:

1) jsde_sdk.jar (also known as jsde##_sdk.jar where ## is the version number,
   such as 92 for 9.2)
2) jpe_sdk.jar (also known as jpe##_sdk.jar where ## is the version number,
   such as 92 for 9.2)

These files are available on your installation of the ArcSDE Java SDK 
from the ArcSDE installation media (usually C:\Program Files\ArcGIS\ArcSDE\lib). 

INSTALLATION

1) Copy all files to the GeoServer library directory.
   In a binary install this is [GEOSERVER_HOME]/server/geoserver/WEB-INF/lib/
   In a WAR install this is [container]/webapps/geoserver/WEB-INF/lib/
2) Restart GeoServer.

COMPATIBILITY

This jar should work with any version of GeoServer based on GeoTools 15.x  
Currently this is anything in 2.9.x. 

For more information see on needed libraries and parameters, see:
http://docs.geoserver.org/latest/en/user/data/database/arcsde.html

For more Geotools related information: 
http://docs.geotools.org/latest/userguide/library/data/arcsde.html

Please report any bugs with jira (https://osgeo-org.atlassian.net/projects/GEOS).

Any other issues can be discussed on the geoserver-users mailing list
(http://lists.sourceforge.net/lists/listinfo/geoserver-users).

