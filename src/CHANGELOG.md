# Change Log

This file is a temporary workaround as our issue tracker is currently unavailable. The issues marked have been resolved.

## GeoServer 2.7.1

Bug:

* GEOS-5857 Fix StringIndexOutOfBoundsException on some WFS-T requests
* GEOS-6182 - Execute goes NPE if the responseForm is missing in the request
* GEOS-6769 - WFS 2.0.0 ListStoredQueries response is invalid
* GEOS-6791 - GeoServerTileLayers are saved even if "Create cached layer" is disabled 
* GEOS-6875 - SLD Validation fails for mixed mode mark names (dynamic symbolizers)
* GEOS-6908 - GeoFence probe fails to reproject allowed area if resource is not EPSG:4326
* GEOS-6943 - 2.7.RC1 Security Regression 

Improvement:

 * GEOS-6658 - MBTiles output format should accept an external file created
 * GEOS-6672 - Creation of a WPS Process for mbtiles generation
 * GEOS-6673 - AbstractTilesGetMapOutputFormat should perform cleanup continuously

Task:

 * GEOS-6659 - GeoPackageProcess should support the configuration of an external file
 * GEOS-6945 - Change JMS clustering groupid to org.geoserver.community

