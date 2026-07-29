GeoServer GeoFence Server Extension
-----------------------------------

This package contains GeoFence's embedded authorization engine, distributed as a separate plug-in.

Please report any bugs with jira (https://osgeo-org.atlassian.net/projects/GEOS).

Any other issues can be discussed on the mailing list (http://lists.sourceforge.net/lists/listinfo/geoserver-users).

Installation
------------

1. Copy the included jar files into the WEB-INF/lib directory of your GeoServer installation, overwriting
   any existing files with the same name.

   This plugin ships `antlr4-runtime-4.13.2.jar`, which GeoFence's persistence layer requires - it is not
   compatible with the older `antlr4-runtime-4.7.1.jar` pulled in by the base install's raster-processing
   support (Jiffle), and having both present at once causes errors. To make this safe even when
   installation is automated (unattended unzip, no one reads this file), the package also includes an
   empty `antlr4-runtime-4.7.1.jar` - copying it over the base install's real one in step 1 removes the
   old version as a side effect of the same copy, with no separate manual step required.

   The trade-off this accepts: Jiffle-based raster band-math (a GeoServer core/Eclipse Imagen feature,
   unrelated to GeoFence) will no longer work in this installation. There is currently no way to have both
   GeoFence's embedded engine and Jiffle-based raster processing working at the same time in the same
   GeoServer instance. If the base install's `antlr4-runtime-4.7.1.jar` is ever renamed/reversioned by an
   upstream change, this stub will silently stop matching it and the conflict returns - worth a quick check
   on any GeoTools/Jiffle version bump.

2. Restart GeoServer.
