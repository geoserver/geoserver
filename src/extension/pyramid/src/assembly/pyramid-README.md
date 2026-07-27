GEOSERVER 2.9+ DATA STORE EXTRAS README
---------------------------------------

This package contains a Pyramid building coverage implementation that is 
distributed as a separate plug-in.  

Please report any bugs with jira (https://osgeo-org.atlassian.net/projects/GEOS).

Any other issues can be discussed on the mailing list (http://lists.sourceforge.net/lists/listinfo/geoserver-users).

Contains the following data stores:

INSTALLATION

1. Copy included gt2-imagepyramid jar included to your 
   GeoServer library directory.  In a binary install this is at 
   [GEOSERVER_HOME]/server/geoserver/WEB-INF/lib/
   In a war install this is [container]/webapps/geoserver/WEB-INF/lib/

2. Restart GeoServer.

imagepyramid should now show up as an option in the web admin tool at 
Config -> Data -> CoverageStores -> New.  Fill out the appropriate params.  For more
information see http://docs.geoserver.org/latest/en/user/data/raster/imagepyramid.html

COMPATIBILITY

This jar should work with any version of GeoServer based on GeoTools 15.x  
Currently this is anything in 2.9.x.  
