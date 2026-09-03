---
render_macros: true
---


# OGC API - Maps

A [OGC API - Maps](https://github.com/opengeospatial/ogcapi-maps) implementation aligned to version 1.0.0, delivering:

- Collection listing, and styles per collection
- Map of a collection, in the default style or in any style associated to it
- Map of the whole dataset, drawing several collections in one image, chosen and ordered with the
  `collections` parameter
- API definition (OpenAPI 3), filtered to the conformance classes enabled on the server
- Optional map parameters, listed under [Requesting a map](#requesting-a-map)
- Attribute filtering of the rendered features, with the queryable attributes of each collection
- Three GeoServer extensions outside the standard: feature info on a map, legends, and the binding of the
  filter parameters to the map resources

Missing functionality at the time of writing:

- Maps specific metadata (e.g., scale ranges)
- A style applied to a dataset map: each collection is drawn in its own default style (not part of the standard)
- Map tilesets (delivered by the separate OGC API - Tiles module)

## OGC API - Maps Implementation status

| [OGC API - Maps](https://github.com/opengeospatial/ogcapi-maps) | Version | Implementation status |
|----|----|----|
| Part 1: Core | [1.0.0](https://docs.ogc.org/is/20-058/20-058.html) | Implemented for collection maps and dataset maps, with the optional classes listed above. |
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
Enabled checkbox. The same tab lists the CQL2 and ECQL filter languages, and the CQL2 capabilities, in two
further tables, plus the **Collections in a dataset map** field described in
[Requesting a map of several collections](#requesting-a-map-of-several-collections).

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
  other parameter classes. Filtering needs `filter` and `mapFilter` together, plus at least one filter language,
  so turning off any of them disables it: none of those classes is then declared in the `conformance` document,
  and neither are the queryables, since there is no filter to write or to describe. When filtering is on instead,
  a `filter-lang` value outside the declared languages is refused with a `400` status, so a filter is never
  dropped without notice.
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

- `filter`, `filter-lang` and `filter-crs`, to render only the features matching a filter. See
  [Filtering a map](#filtering-a-map) below.

Every response carries the delivered area back in the `Content-Crs`, `Content-Bbox`, `Content-Orientation` and
`Content-Datetime` headers. `Content-Bbox` follows the axis order of the CRS authority, so for `EPSG:4326` it reads
latitude,longitude, and it always reports the area before any rotation. `Content-Crs` holds the CRS URI between
angle brackets, for example `<http://www.opengis.net/def/crs/EPSG/0/4326>`, and is omitted when the CRS has no
authority code, for example when using an AUTO code. `Content-Orientation` is the rotation applied, zero for a map with the default orientation.

The CRSs a map can be delivered in are advertised as CRS URIs by the collection description, in its `crs`
property, and by the `/collections` document, which lists them once at its root so that each collection can
point at that single list with `#/crs`. The list is the **Services > WMS** SRS list when one is configured,
otherwise it's all the known codes. A collection whose storage CRS is
not CRS84 also reports it as `storageCrs`, adding it to the `crs` list, and repeats its extent in that CRS under
`extent.spatial.storageCrsBbox`. That is the CRS a map is delivered in when the request does not ask for another
one, so no reprojection takes place.

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
  `rule`, `lang`, `transparent`, `bgcolor` and `legend-options` parameters as the WMS `GetLegendGraphic` request.

## Requesting a map of several collections

A map of the whole service is retrieved from `/ogc/maps/v1/map`, linked from the landing page. It takes the same
parameters as a collection map, plus `collections`, a comma separated list choosing what to draw:

```
/ogc/maps/v1/map?collections=topp:states,topp:tasmania_roads&f=image/png
```

The collections are drawn in painter's order, the first one at the bottom and the last one on top. Each entry is
either a collection identifier or the full URL of its collection resource. A layer group counts as a single
collection, and keeps its own internal layer order and per-layer styles. Every collection is drawn in its own
default style: OGC API - Maps 1.0.0 has no way to pick a style per collection.

Without `collections` the map draws the contents of the service it was addressed to: the layer or layer group of
a [virtual service](../../../configuration/virtual-services.md), or a selection of the collections of the
workspace, or of the whole catalog at the root. Drawing a whole catalog would be both slow and the map likely unreadable, so the
map holds at most the number of collections set in **Services > WMS > Maps** (10 by default). The collections are picked
in two steps: first the layer groups, being curated, then the layers by name until the limit is reached, leaving out any
layer already drawn as a member of one of those groups. The selection is then stacked bottom to top as rasters,
polygon layers, layer groups, lines and points, so that the least covering layers stay on top. Ask for the
collections you want in `collections` to get anything else: that list is never cut.

What the WMS capabilities document leaves out is left out here too: disabled layers, layers not advertised, vector
layers without a geometry, and layer groups nested in another group. A group in `CONTAINER` mode cannot be drawn on
its own, as in WMS, so its members are collections instead. A layer drawn as part of a group is not drawn a second
time on its own, whatever the mode of the group; ask for it in `collections` to have it drawn separately.

Naming a collection that does not exist, or one that cannot be drawn, fails with a `400` status.

The landing page describes what the dataset map covers, in an `extent` property built like the one of a
collection, and the CRSs it can be delivered in, in a `crs` property. The extent covers the default contents,
in CRS84, and reports no temporal range: working one out would mean reading the time domain of every collection.

The dimension parameters apply as they do in WMS: one `datetime` or `subset` value drives every collection having
that dimension, and the collections that do not have it are drawn whole. A request fails only when none of the
collections has the dimension asked for. A `filter` is applied to every collection drawn, so it is only useful on
collections sharing the attributes it names.

The HTML view of the dataset map is an interactive preview with a collection palette: pick the collections to
draw from the list of candidates, and move them up and down to change the drawing order. `.../map/info`, the
GeoServer feature info extension, works on the dataset map too, reporting across the collections drawn.

## Service limits

OGC API - Maps 1.0.0 is subject to the same service limits as WMS (memory, rendering time, max dimensions, and so on).

## Filtering a map

Filtering is not part of OGC API - Maps 1.0.0. GeoServer applies the filter parameters of
[OGC API - Features - Part 3: Filtering](https://docs.ogc.org/is/19-079r2/19-079r2.html) to the map resources,
so only the features matching the filter are rendered. Binding those parameters to the map resources is itself a
GeoServer extension, declared as a separate conformance class. The request parameters are:

- `filter`, the filter expression.
- `filter-lang`, its language: `cql2-text` (the default), `cql2-json`, or `ecql-text`, the GeoServer own
  Extended CQL. XML filters and feature identifiers are not supported, though they might be added in the future.
  Only the languages enabled in the service configuration are accepted, the API document lists them in the
  `filter-lang` parameter, and any other value gives a `400` status.
- `filter-crs`, the CRS of the geometry literals used in spatial predicates. Defaults to CRS84.

For example, to render only the roads whose type is a motorway:

```
.../collections/topp:roads/map?f=image/png&filter=type='motorway'
```

The filter is combined with `bbox`, `datetime` and `subset` by AND, so a feature is drawn only when it satisfies
all of them. The same parameters work on the `.../map/info` resource, so the feature info reports only the
features that the map shows. An invalid expression, an unknown language, or an unknown CRS is answered with a
`400` status.

The attributes that can be used in a filter, with their types, are listed by the queryables resource:

```
.../collections/topp:roads/queryables
```

It answers a JSON Schema document (`application/schema+json`), also available as HTML, and is linked from the
collection description. A vector layer lists the attributes of its features. A raster layer lists them only when
it is structured, an image mosaic for example: the filter then selects which files to mosaic, using the attributes
of the index, such as the file location, the time of ingestion or the elevation. Other raster layers, and layer
groups, answer with a `404` status, a layer group because each of its members has its own set of attributes.

A filter naming an attribute that none of the collections drawn lists as a queryable is answered with a `400`
status, rather than being quietly dropped, so a typo in an attribute name shows up right away. On a map of
several collections, or on a layer group, the attribute only has to be a queryable of one of them, since the
collections can have different schemas, and it is applied to every one of them.

The map preview allows to pick a filter and a filter language in its tool box.

