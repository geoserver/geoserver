# Change Log

This file is a temporary workaround as our issue tracker is currently unavailable. The following issues are resolved.

## Release Notes GeoServer 2.6.3

Bug:

 * GEOS-5857 - Fix StringIndexOutOfBoundsException on some WFS-T requests
 * GEOS-6949 - Caching defaults has metatile size 15 shown as 16
 * GEOS-6182 - Execute goes NPE if the responseForm is missing in the request
 * GEOS-6811 - regression in allowed style and other names
 * GEOS-6134 - WFS 2.0 cannot output GML 3.1
 * GEOS-6182 - Execute goes NPE if the responseForm is missing in the request
 * GEOS-6555 - Restrict FormatOptionsKvpParser parsers usage by service (WMS or WFS)
 * GEOS-6610 - Javadoc Download lacks CSS
 * GEOS-6744 - Wind style (amplitude and direction on 2 bands geotiff) doesn't work anymore
 * GEOS-6807 - WCS GetCapabilties requests with Sections parameter throws an exception
 * GEOS-6845 - Regression: cannot configure anymore coverages whose name is not xml safe 
 * GEOS-6848 - When the "Linearization Tolerance" is not empty geoserver throws an error when publishing data with EPSG:4326
 * GEOS-6867 - Filter validation fails in WFS when a join needs to be reordered, and has local, non joining filters
 * GEOS-6869 - Join filter validator applies the joining capabilities checks also to non joining filters
 * GEOS-6873 - GeoServer leaking commons http-client connection pools in ResourcePool
 * GEOS-6884 - Meters uom and hatch fill in PolygonSymbolizer creates Rendering process failed error
 * GEOS-6923 - Slow JSON response on EPSG:900913 defined vector layer
 * GEOS-6934 - gwc fails to render 900913 gridset with ImageMosaic store and the contour style

Improvement:

 * GEOS-5333 - Be explicit about the 50 items records in the preview page description

Task:

 * GEOS-5509 - Quickstart -> styling A map



