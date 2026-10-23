# MapLibre Tiles (MLT)

GeoServer can produce [MapLibre Tiles](https://github.com/maplibre/maplibre-tile-spec) as a WMS/WMTS output format, next to the Mapbox Vector Tiles of the Vector Tiles extension. MLT stores attributes in columns and compresses them, so tiles are usually smaller than the equivalent Mapbox Vector Tile.

!!! note
    This is an output format, not a data source.

!!! warning
    The [MapLibre Tile specification](https://maplibre.org/maplibre-tile-spec/specification/) is not versioned. It is a live document that keeps changing, and parts of it are marked as experimental, so the tile format can change from one release to the next.

## Requirements

This module needs Java 21. The MapLibre Tile library it uses is published as Java 21 bytecode, while the rest of GeoServer runs on Java 17 or later. When using Java 17 with this module, GeoServer will not start at all.

## Installation

As a community module, the package needs to be downloaded from the [nightly builds](https://build.geoserver.org/geoserver/), picking the community folder of the corresponding GeoServer series (e.g. if working on the GeoServer main development branch nightly builds, pick the zip file from `main/community-latest`).

To install the module, unpack the zip file contents into the GeoServer `WEB-INF/lib` directory and restart GeoServer.

The Vector Tiles extension must be installed as well, the MLT module builds on it.

## Usage

MLT is meant to be used as a tiled format. In the layer configuration, open the *Tile Caching* tab, tick `application/vnd.maplibre-vector-tile` among the cached formats, and add the `WebMercatorQuad` gridset to the layer. `WebMercatorQuad` is the OGC name of the tile matrix set that XYZ clients use.

!!! note
    The specification does not define a media type for MLT, nor a file extension. GeoServer uses `application/vnd.maplibre-vector-tile` and `.mlt` (to parallel MVTs). It is not registered with IANA and may change once the specification picks one.

The tiles can then be fetched from WMTS with the usual `{z}`, `{y}` and `{x}` placeholders, so the URL can be handed to a client as a template:

```
...http://localhost:8083/geoserver/gwc/service/wmts/rest/myLayer/
WebMercatorQuad/{z}/{y}/{x}?format=application/vnd.maplibre-vector-tile
```

The generation options of the Vector Tiles extension apply to MLT as well. See [Vector Tiles Generation Options](../../extensions/vectortiles/options.md).

## Attribute types

MLT stores each attribute in a typed column and picks the smallest type that holds the values of the tile. An attribute declared as a 64 bit integer can therefore come back as a 32 bit integer, with the same value. Attribute values whose type does not match the column are converted to the column type, so a layer mixing numbers and text in one attribute returns text.

MLT columns hold text, boolean, integer and floating point values. Other values are converted: dates, times and anything else become text, while other numbers become floating point. An attribute with no value on a feature is left out of that feature.

## Feature identifiers

GeoServer feature identifiers usually end with a number, such as `states.7`, and that number becomes the MLT feature identifier. Features whose identifier ends in anything else are written without one, which is allowed and does not affect the other features of the tile.

## Mixed geometry types

A layer whose geometry column holds different geometry types works. The one geometry the encoder cannot write is a geometry collection mixing types, such as a point and a line in the same feature. Such a feature is written as one feature per member instead, repeating the attributes and the feature identifier, so a client that tracks features by identifier, for hover or selection effects, matches several features at once.

Multi point, multi line and multi polygon geometries are written as a single feature, so the `vt-coalesce` generation option, which merges features sharing the same attributes, does not cause this by itself. It only does when the merged features have different geometry types.
