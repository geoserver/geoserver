---
render_macros: true
---


# OGC API - Maps

A [OGC API - Maps](https://github.com/opengeospatial/ogcapi-maps) implementation aligned to version 1.0.0, delivering:

- Collection listing, and styles per collection
- Map of a collection, in the default style or in any style associated to it
- API definition (OpenAPI 3), filtered to the conformance classes enabled on the server
- Optional map parameters: spatial subsetting, general subsetting (elevation and custom dimensions), scaling, display resolution, datetime, output CRS, background and orientation
- Two GeoServer extensions outside the standard: feature info on a map, and legends

Missing functionality at the time of writing:

- Maps specific metadata (e.g., scale ranges)
- Maps of the whole dataset, and multi-collection maps
- Map tilesets (delivered by the separate OGC API - Tiles module)

## OGC API - Maps Implementation status

| [OGC API - Maps](https://github.com/opengeospatial/ogcapi-maps) | Version | Implementation status |
|----|----|----|
| Part 1: Core | [1.0.0](https://docs.ogc.org/is/20-058/20-058.html) | Implemented for collection maps, with the optional classes listed above. |
| Part 2: Partitioning | [Draft](https://github.com/opengeospatial/ogcapi-maps/tree/master/extensions/partitioning/standard) | Implementation based on early specification draft. |

## Installing the GeoServer OGC API - Maps module

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download `ogcapi-maps` zip archive.

    - {{ snapshot }} example: [ogcapi-maps](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-ogcapi-maps-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

    !!! warning
        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-{{ snapshot }}-ogcapi-maps-plugin.zip).

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

4.  Restart GeoServer.

    On restart the services are listed at <http://localhost:8080/geoserver>

## Configuration of OGC API - Maps module

The module shares the same mapping engine as WMS, follows the same configuration and exposes the same layers. As a significant difference, Maps does not have a concept of layer tree, so only individual layers and groups can be exposed.

### Turning optional functionality off

As with other OGC APIs, Maps exposes conformance classes, which can be turned off individually from the
**Services > WMS** settings page in the admin UI, on the **Maps** tab: it lists each optional class with an
Enabled checkbox, covering the dataset map and its collection selection, spatial subsetting, general subsetting,
scaling, display resolution, datetime, output CRS, background, orientation, the TIFF and SVG output formats,
filtering and queryables, and the three GeoServer extensions: feature info, legend, and the class binding the
filter parameters to the map resources, without which filtering does not work at all. The same tab lists the CQL2
and ECQL filter languages, and the CQL2 capabilities, in two further tables.

The **Collections in a dataset map** field on that tab is the count described in
[Requesting a map of several collections](#requesting-a-map-of-several-collections). Leave it empty for the
default of ten.

Disabling a class removes it from the `conformance` document and removes its parameters from the API document.
What happens to a request that still uses the disabled functionality depends on the kind of class:

- A parameter class (`spatialSubsetting`, `generalSubsetting`, `scaling`, `displayResolution`, `datetime`,
  `crs`, `background`, `orientation`): the parameter is ignored, following the OGC API convention of ignoring
  unsupported query parameters, and the map is returned as if it had not been provided. With `generalSubsetting`
  off, the elevation and custom-dimension axes of a `subset` are dropped rather than rejected.
- An output format class (`tiff`, `svg`): the format is no longer offered, so a request for it fails HTTP content
  negotiation with a `406` status.
- The two GeoServer extension resources (`featureInfo`, `legend`): the resource is removed from the API document
  and answers with a `404` status, since it is not part of the standard.
- `datasetMap`: the `/map` resource and its feature info are removed from the API document and answer with a
  `404` status, the landing page stops linking them and stops publishing an `extent`. Collection maps are not
  affected.
- `collectionsSelection`: the `collections` parameter is ignored, so the dataset map always draws the default
  contents, and the preview offers no palette. The class needs `datasetMap` to mean anything, so turning that off
  disables both, and neither is then declared in the `conformance` document.
- The filter classes (`filter`, `mapFilter`, and the language ones): the `filter` parameter is ignored, like the
  other parameter classes. Filtering needs `filter` and `mapFilter` together, so turning off either one disables
  it: neither class is then declared in the `conformance` document, and neither are the filter languages nor the
  queryables, since there is no filter to write or to describe. A filter written in a language that is not
  declared is ignored too.
- `queryables`: the resource is removed from the API document, the link disappears from the collection
  description, and a request for it answers with a `404` status. Turning filtering off has the same effect,
  since on a map the queryables exist only to describe what the `filter` parameter accepts.


## Requesting a map

A map of a collection is retrieved from `/ogc/maps/v1/collections/{collectionId}/map`, or from
`.../styles/{styleId}/map` to pick a style other than the default one. The output format is chosen with the `f`
parameter, PNG and JPEG are always available, TIFF and SVG can be turned off via conformance classes,
in general, other output formats can be removed in the mapping configuration panel as well.

The area, size and appearance of the map are controlled by the parameters of the optional conformance classes:

- `bbox` and `bbox-crs`, `subset` and `subset-crs`, or `center` and `center-crs`, to pick the area. A `center`
  request also needs `width`, `height` and `scale-denominator`, since a point alone does not define an extent.
- `subset` can be also used to select along dimensions beyond space and time, when general subsetting is enabled: the
  elevation and any custom dimension declared on the layer, for example `subset=elevation(500)` or
  `subset=<dimension>(<value>)`, with `low:high` for a range. The dimensions available on a collection, and
  their ranges, are listed in the collection description under `extent`. An axis that is not a dimension of the
  collection is rejected with a `400` status.
- `width` and `height`, or `scale-denominator`, to size the image. When only one of width and height is given,
  the other is computed from the scale denominator.
- `mm-per-pixel`, the size of a pixel on the display, 0.28 mm when not given.
- `datetime`, to select a time in a layer with a time dimension.
- `crs`, the CRS of the delivered map. Both the extended `http://www.opengis.net/def/crs/EPSG/0/4326` form and the SafeCURIE `[EPSG:4326]` form are
  accepted; these forms follow the axis order declared by the CRS authority, while a plain `EPSG:4326` is always
  longitude/latitude. The same holds for `bbox-crs`, `subset-crs` and `center-crs`.
- `bgcolor`, `transparent`, `void-color` and `void-transparent`, for the map background.
- `orientation`, to rotate the map, in degrees.

Every response carries the delivered area back in the `Content-Crs`, `Content-Bbox`, `Content-Orientation` and
`Content-Datetime` headers. `Content-Bbox` follows the axis order of the CRS authority, so for `EPSG:4326` it reads
latitude,longitude, and it always reports the area before any rotation. `Content-Crs` holds the CRS URI between
angle brackets, for example `<http://www.opengis.net/def/crs/EPSG/0/4326>`, and is omitted when the CRS has no
authority code, for example when using an AUTO code. `Content-Orientation` is the rotation applied, zero for a map with the default orientation.

Two extra resources are GeoServer extensions, not part of the standard:

- `.../map/info`, the feature information at a pixel, selected with the `i` and `j` parameters. `limit` sets the
  maximum number of features returned, one by default, counted across all the layers of the collection: a layer
  group needs a higher limit to report something from more than one of its layers. The pixel is read from the
  same map the `map` resource returns, so all the map parameters above apply here too, and the reported features
  are the ones the map shows. `orientation` is the exception, it is currently ignored: the pixel is always read
  from the map as it would be drawn without rotation, so on a rotated map use the `map` resource to see the
  rotation and this one to query it. When no `bbox-crs` is given, `crs` applies to the `bbox` as well.
  Unless otherwise specified, the parameters supported by a WMS GetFeatureInfo are also available in the `.../map/info` resource.
- `.../legend` and `.../styles/{styleId}/legend`, the legend of a style, with the same `width`, `height`, `scale`,
  `rule`, `lang`, `transparent`, `bgcolor` and `legend-options` parameters as the WMS `GetLegendGraphic` request (as well as other parameters supported by it).

## Installing the GeoServer OGC API - Maps module

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download `ogcapi-maps` zip archive.

    - {{ snapshot }} example: [ogcapi-maps](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-ogcapi-maps-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

    !!! warning
        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-{{ snapshot }}-ogcapi-maps-plugin.zip).

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

4.  Restart GeoServer.

    On restart the services are listed at <http://localhost:8080/geoserver>

## Configuration of OGC API - Maps module

The module is based on the GeoServer WMS one, follows the same configuration and exposes the same layers. As a significant difference, Maps does not have a concept of layer tree, so only individual layers and groups can be exposed.

### Turning optional functionality off

The optional conformance classes are all enabled by default. They can be turned off individually from the
**Services > WMS** settings page in the admin UI, on the **Maps** tab: it lists each optional class with an
Enabled checkbox, covering spatial subsetting, general subsetting, scaling, display resolution, datetime,
output CRS, background, orientation, the TIFF and SVG output formats, and the two GeoServer extensions,
feature info and legend.

Disabling a class removes it from the `conformance` document and removes its parameters from the API document.
What happens to a request that still uses the disabled functionality depends on the kind of class:

- A parameter class (`spatialSubsetting`, `generalSubsetting`, `scaling`, `displayResolution`, `datetime`,
  `crs`, `background`, `orientation`): the parameter is ignored, following the OGC API convention of ignoring
  unsupported query parameters, and the map is returned as if it had not been provided. With `generalSubsetting`
  off, the elevation and custom-dimension axes of a `subset` are dropped rather than rejected.
- An output format class (`tiff`, `svg`): the format is no longer offered, so a request for it fails HTTP content
  negotiation with a `406` status.
- The two GeoServer extensions (`featureInfo`, `legend`): the resource is removed from the API document and answers
  with a `404` status, since it is not part of the standard.
