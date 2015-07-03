# Change Log

This file is a temporary workaround as our issue tracker is currently unavailable. The issues marked have been resolved.

## GeoServer 2.8-beta

Bug:

 * GEOS-5857 Fix StringIndexOutOfBoundsException on some WFS-T requests
 * GEOS-5393 - Build hangs in ResourceAccessManagerWFSTest.testInsertNoLimits if another GeoServer is listening on localhost:8080
 * GEOS-6870 - Malformed Path to Resource on Windows
 * GEOS-6934 - gwc fails to render 900913 gridset with ImageMosaic store and the contour style

Improvement:

* GEOS-4833 - SetCharacterEncodingFilter is same as SpringFramework CharacterEncodingFilter
* GEOS-6905 - Support embedding WCS 2.0 requests in WPS
* GEOS-6946 - Allow cql expressions in ColorMapEntry for GetLegendGraphic

New Feature:

 * GEOS-6901 - OGR based WPS output formats


Task:

 * GEOS-6960 - Update GS-code after viewsManager classes cleanup
