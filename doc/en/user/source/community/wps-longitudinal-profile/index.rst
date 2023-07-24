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
#. geometry - geometry in wkt or ewkt format, along which the altitude profile will be created. If wkt is used, its CRS will be assumed as CRS of coverage.
#. distance - maximal distance between points of altitude profile

**Optional:**

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
