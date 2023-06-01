.. _wpslongitudinal:

WPS longitudinal profile process
================================

WPS longitudinal profile process provides ability to calculate elevation profile for linestring
* Reproject result to different CRS
* Adjust elevation profile based on additional layer

Installing the WPS longitudinal profile process
-----------------------------------------------

The WPS longitudinal profile extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

#. From the :website:`website download <download>` page, locate your release, and download: :download_extension:`wps-longitudinal-profile`
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   Make sure you do not create any sub-directories during the extraction process.

#. Restart GeoServer.


Module description
------------------

This module provides longitudinal profile process.
Process accepts following parameters:
Required:
1. layerName - name of the raster layer (coverage) which will be used, for elevation profile creation
2. linestringWkt - linestring in wkt format, along which the elevation profile will be created
3. distance - maximal distance between points of elevation profile
Optional:
1. adjustmentLayerName - name of the layer with elevation, which will be used to adjust elevation values
2. projection - target CRS of result
3. elevationIndex - index of elevation field in the array of coverage coordinates (0 by default)
4. elevationName - name of the elevation attribute on adjustment layer feature type

The process splits provided linestring into into segments of no more then provided distance length.
Then evaluates elevation for each point and builds longitudinal profile. If adjustment layer name
is provided, elevation will be adjusted by searching feature that corresponding point contains, and
getting it's elevation attribute, further subtracting it from elevation received from coverage.
If projection parameter is provided, points of profile will be reprojected to taget CRS.

Response contains following objects:
profile - contains array of points of the profile
infos - general info on process result

profile object contains array of points
each point has following values:
distance - distance to this point from the beginning of the profile (first point)
x - x coordinate of point
y - y coordinate of point
altitude - altitude of this point
slope - slope between previous and current elevation

infos object fields:
elevationPositive - sum of positive elevations on this profile
elevationNegative - sum of negative elevations on this profile
distance - total length of profile
firstpointX - x coordinate of first point
firstpointY - y coordinate of first point
lastpointX - x coordinate of last point
lastpointY - y coordinate of last point
representation - target CRS of resulting points
processedpoints - total number of processed points
executedtime - duration of process execution in milliseconds
