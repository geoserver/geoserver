GeoServer IAU Authority Extension
=================================

This package contains the International Astronomical Union CRS database,
with over 2000 CRSs covering planetary bodies.

Please report any bugs with [jira](https://osgeo-org.atlassian.net/projects/GEOS).

Any other issues can be discussed on the [mailing list](http://lists.sourceforge.net/lists/listinfo/geoserver-users).

Installation
------------

1. Copy included gt-iau-wkt jar included to your 
   GeoServer library directory.  In a binary install this is at 
   [GEOSERVER_HOME]/server/geoserver/WEB-INF/lib/
   In a war install this is [container]/webapps/geoserver/WEB-INF/lib/

2. Restart GeoServer.

3. The IAU codes now show up in the "SRS list" demo page. It is now possible
   to publish data in such CRSs, and have the IAU codes embedded in various
   types of outputs, including GML, GeoJSON, Shapefile, GeoTIFF.
