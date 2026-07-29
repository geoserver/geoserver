GeoServer GeoFence Server Extension
-----------------------------------

This package contains GeoFence's embedded authorization engine, distributed as a separate plug-in.

Please report any bugs with jira (https://osgeo-org.atlassian.net/projects/GEOS).

Any other issues can be discussed on the mailing list (http://lists.sourceforge.net/lists/listinfo/geoserver-users).

Installation
------------

1. Copy the included jar files into the WEB-INF/lib directory of your GeoServer installation.

2. **Remove any older `antlr4-runtime-4.7.*.jar`** already present in WEB-INF/lib (it is pulled in by
   the base GeoServer install's raster-processing support). This plugin ships `antlr4-runtime-4.13.2.jar`,
   which GeoFence's persistence layer requires; having both versions present at once is not supported and
   will cause errors. Only remove the older jar - do not remove `antlr4-runtime-4.13.2.jar` itself.

   Removing the older jar means Jiffle-based raster band-math (a GeoServer core/Eclipse Imagen feature,
   unrelated to GeoFence) will no longer work in this installation. There is currently no way to have both
   GeoFence's embedded engine and Jiffle-based raster processing working at the same time in the same
   GeoServer instance.

3. Restart GeoServer.
