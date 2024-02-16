# Process Cookbook {: #wps_processes }

The Web Processing Service describes a method for publishing geospatial processes, but does not specify what those processes should be. Servers that implement WPS therefore have complete leeway in what types of processes to implement, as well as how those processes are implemented. This means that a process request designed for one type of WPS is not expected to work on a different type of WPS.

GeoServer gathers processes into several different categories based on subject. These categories are grouped by prefix:

-   geo: geometry processes
-   ras: raster processes
-   vec: Vector processes
-   gs: GeoServer-specific processes

This cookbook provides examples of some of the available process. Unless otherwise stated examples were generated with the [WPS Request Builder](../requestbuilder.md) using the sample data included with each GeoServer release.

-   [Geometry Processes](geo.md)
-   [GeoServer processes](gs.md)
-   [Process chaining](chaining.md)

!!! note

    Previous releases of GeoServer grouped processes not by subject, but by the internal library responsible for implementation. The "JTS" and "gt" prefixes can be enabled to preserve backwards compatibility, or you may safely disable them off - their functionality is correctly sorted into the "vec" and "geo" categories.
