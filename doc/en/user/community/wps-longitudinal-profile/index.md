# WPS longitudinal profile process

WPS longitudinal profile process provides the ability to calculate an altitude profile for the specified linestring.

## In addition, the process can:

- Reproject result to different CRS
- Adjust altitude profile based on additional layer
- Measure distances on the ground (default) or on the plane of a projected CRS

## Installing the WPS longitudinal profile process

1.  If you haven't done already, install the WPS extension: [Installing the WPS extension](../../services/wps/install.md).

2.  Download the WPS longitudinal profile process extension from the [nightly GeoServer community module builds](https://build.geoserver.org/geoserver/main/community-latest/).

    !!! warning
        Make sure to match the version of the extension to the version of the GeoServer instance!

3.  Extract the contents of the archive into the `WEB-INF/lib` directory of the GeoServer installation.

## Module description

This module provides longitudinal profile process. The process splits provided geometry (for example linestring) into segments of no more then provided distance length. Then evaluates altitude for each point and builds longitudinal profile. If adjustment layer name is provided, altitude will be adjusted by searching feature that contains corresponding point, and getting it's altitude attribute, further subtracting it from altitude received from coverage. If targetProjection parameter is provided, points of profile will be reprojected to target CRS, otherwise to CRS of provided ewkt geometry.

Process accepts following parameters:

### Required:

1.  layerName - name of the raster layer (coverage) which will be used for altitude profile creation
2.  coverage - the actual raster to be used for altitude profile creation. This is an alternative to layerName, allows for process chaining (a chained process might have computed the input coverage)
3.  geometry - geometry in wkt or ewkt format, along which the altitude profile will be created. If wkt is used, its CRS will be assumed as CRS of coverage.

### Optional:

1.  distance - maximum distance (in meters, measured on the ground) between consecutive points in the altitude profile. If not specified, this distance will be automatically determined based on the coverage resolution, by calculating the diagonal length of a pixel.
2.  adjustmentLayerName - name of the layer with altitude, which will be used to adjust altitude values. Layer should have polygon or multipolygon geometry, and altitude attribute. Layer should be configured in the GeoServer
3.  targetProjection - target CRS of result.
4.  altitudeIndex - index of altitude field in the array of coverage coordinates (0 by default)
5.  altitudeName - name of the altitude attribute on adjustment layer feature type
6.  projectedDistance - when true, distances are measured on the targetProjection plane rather than on the ground (false by default). See [Distances and slopes](#distances-and-slopes).

### Response contains following objects:

1.  profile - contains array of points of the profile
2.  infos - general info on process result

The profile object contains an array of points.

### Each point has following values:

1.  totalDistanceToThisPoint - distance to this point from the beginning of the profile (first point)
2.  x - x coordinate of point
3.  y - y coordinate of point
4.  altitude - altitude of this point
5.  slope - slope percentage between previous and current point, the altitude difference over the distance from the previous point

### Infos object fields:

1.  altitudePositive - sum of positive altitudes on this profile
2.  altitudeNegative - sum of negative altitudes on this profile
3.  distance - total length of profile
4.  firstpointX - x coordinate of first point
5.  firstpointY - y coordinate of first point
6.  lastpointX - x coordinate of last point
7.  lastpointY - y coordinate of last point
8.  representation - target CRS of resulting points
9.  processedpoints - total number of processed points
10. executedtime - duration of process execution in milliseconds

## Distances and slopes

By default distances are measured on the ground, along the ellipsoid, whatever the targetProjection. A profile has the same length whether its points are returned in the native CRS of the coverage, in EPSG:4326 or in EPSG:3857. This avoids the scale distortion of projections: in particular EPSG:3857 stretches distances by about 40% at a latitude of 45 degrees, and should never be used to measure lengths.

When **projectedDistance** is true, distances are instead measured on the plane of the targetProjection, as straight lines between the returned coordinates. Use it when distances in a projected CRS have a value of their own, e.g. a legal one in a national grid. It is to be noted that the result then carries the scale distortion of that projection. **The targetProjection must be a projected CRS**: when targetProjection is missing, the CRS of the ewkt geometry is used, or the coverage CRS for a wkt geometry. A geographic CRS is rejected with an error, since a straight line in degrees is not a distance.

In both cases:

- Distances are expressed in the unit of the targetProjection, e.g. feet for a CRS in feet, and in meters for geographic CRSs, since degrees are not a ground unit.
- The distance parameter is always in meters on the ground, and only controls how densely the line is sampled.
- Slopes compare altitudes and distances in the same unit, so they do not depend on the unit of the targetProjection. Altitudes are taken in the unit declared for the coverage band (e.g. feet), when missing in the unit of the coverage CRS if projected, and in meters otherwise. Altitudes are reported as found in the coverage, without conversion.

!!! note
    It's possible to set wpsLongitudinalMaxThreadPoolSize (integer value) environment variable to limit the size of the extension's thread pool. It's possible to set wpsLongitudinalVerticesChunkSize (integer value) environment variable to define number of vertices processed in a chunk.

## Tunables and safeguards

The WPS longitudinal profile process is designed to be used with large datasets and may extract many points from the input geometry. To avoid performance issues, the process has a number of tunables and safeguards, that the system administrator can specify as system or environment variables:

- `wpsLongitudinalMaxPoints`: maximum number of points to be extracted from the input geometry. The default value is 50000 (amounts to a memory usage of a few megabytes).
- `wpsLongitudinalMaxThreadPoolSize`: size of the background thread pool used by the process to speed up calculations (all calls use the same pool). The default value matches the numbers of CPU threads.
- `wpsLongitudinalVerticesChunkSize``: number of vertices processed in a single background thread call. The default value is 5000.
