.. _wpslongitudinal:

WPS longitudinal profile process
================================

WPS longitudinal profile process provides the ability to calculate an altitude profile for the specified linestring.

**In addition, the process can:**

* Reproject result to different CRS
* Adjust altitude profile based on additional layer

Installing the WPS longitudinal profile process
-----------------------------------------------

 #. If you haven't done already, install the WPS extension: :ref:`wps_install`.

 #. Download the WPS longitudinal profile process extension from the `nightly GeoServer community module builds <https://build.geoserver.org/geoserver/main/community-latest/>`_.

    .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

 #. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.


Module description
------------------

This module provides longitudinal profile process.
The process splits provided geometry (for example linestring) into segments of no more then provided distance length.
Then evaluates altitude for each point and builds longitudinal profile.
If adjustment layer name is provided, altitude will be adjusted by searching feature that contains corresponding point,
and getting it's altitude attribute, further subtracting it from altitude received from coverage.
If targetProjection parameter is provided, points of profile will be reprojected to target CRS, otherwise to CRS
of provided ewkt geometry.

Process accepts following parameters:

**Required:**

#. layerName - name of the raster layer (coverage) which will be used for altitude profile creation
#. coverage - the actual raster to be used for altitude profile creation. This is an alternative to layerName, allows for process chaining (a chained process might have computed the input coverage)
#. geometry - geometry in wkt or ewkt format, along which the altitude profile will be created. If wkt is used, its CRS will be assumed as CRS of coverage.

**Optional:**

#. distance - maximum distance (in meters) between points in the altitude profile. If not specified, this distance will be automatically determined based on the coverage resolution, by calculating the diagonal length of a central pixel.
#. adjustmentLayerName - name of the layer with altitude, which will be used to adjust altitude values. Layer should have polygon or multipolygon geometry, and altitude attribute. Layer should be configured in the GeoServer
#. targetProjection - target CRS of result
#. altitudeIndex - index of altitude field in the array of coverage coordinates (0 by default)
#. altitudeName - name of the altitude attribute on adjustment layer feature type

**Response contains following objects:**

#. profile - contains array of points of the profile
#. infos - general info on process result

The profile object contains an array of points.

**Each point has following values:**

#. totalDistanceToThisPoint - distance to this point from the beginning of the profile (first point) in units of CRS
#. x - x coordinate of point
#. y - y coordinate of point
#. altitude - altitude of this point
#. slope - slope between previous and current altitude

**Infos object fields:**

#. altitudePositive - sum of positive altitudes on this profile
#. altitudeNegative - sum of negative altitudes on this profile
#. distance - total length of profile
#. firstpointX - x coordinate of first point
#. firstpointY - y coordinate of first point
#. lastpointX - x coordinate of last point
#. lastpointY - y coordinate of last point
#. representation - target CRS of resulting points
#. processedpoints - total number of processed points
#. executedtime - duration of process execution in milliseconds


.. note::
   It's possible to set wpsLongitudinalMaxThreadPoolSize (integer value) environment variable to limit the size of the extension's thread pool.
   It's possible to set wpsLongitudinalVerticesChunkSize (integer value) environment variable to define number of vertices processed in a chunk.

Tunables and safeguards
-----------------------

The WPS longitudinal profile process is designed to be used with large datasets and may extract
many points from the input geometry. To avoid performance issues, the process has a number of tunables and safeguards,
that the system administrator can specify as system or environement variables:

* ``wpsLongitudinalMaxPoints``: maximum number of points to be extracted from the input geometry. The default value is 50000 (amounts to a memory usage of a few megabytes).
* ``wpsLongitudinalMaxThreadPoolSize``: size of the background thread pool used by the process to speed up calculations (all calls use the same pool). The default value matches the numbers of CPU threads.
* ``wpsLongitudinalVerticesChunkSize```: number of vertices processed in a single background thread call. The default value is 5000.