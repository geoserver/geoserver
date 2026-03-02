# Geometry Processes

The geometry processes are built using the [JTS Topology Suite](http://tsusiatsoftware.net/jts/main.html) (JTS). JTS is a Java library of functions for processing geometries in two dimensions. JTS conforms to the Simple Features Specification for SQL published by the Open Geospatial Consortium (OGC), similar to PostGIS. JTS includes common spatial functions such as area, buffer, intersection, and simplify.

GeoServer WPS implements some of these functions as "geo" processes. The names and definitions of these processes are subject to change, so they have not been included here. For a full list of JTS processes, please see the GeoServer [WPS capabilities document](../operations.md#wps_getcaps) or browse with the [WPS Request Builder](../requestbuilder.md).
