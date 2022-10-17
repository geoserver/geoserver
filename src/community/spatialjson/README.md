# SpatialJSON WFS Output Format Extension

This module adds the SpatialJSON WFS output format. The SpatialJSON format is a more compact and
memory-friendly variant of GeoServer's GeoJSON format. It aims to save space by applying several
optimizations to traditional GeoJSON format for simple feature results. Most of these optimizations
work by removing redundand information from the JSON-encoded features.

A service exception is thrown if the result contains complex features as the SpatialJSON format
does not handle those.

> **Note**: The SpatialJSON format is **not compatible** with GeoJSON. A SpatialJSON enabled reader
> is required to decode features transferred in SpatialJSON format.

This module adds two additional WFS output formats for requesting simple features in SpatialJSON
format:

- `application/json; subtype=json/spatial` for requesting SpatialJSON
- `text/javascript; subtype=json/spatial` for requesting SpatialJSON as a JSONP request

> **Warning**: At the time of writing, this format is still _work in progress_ and changes may be
> applied in the future.

### Development Status

The SpatialJSON format is still a playground for implementing several optimizations to transfer
even huge amounts of spatial data from the server to the client efficiently:

1. **Opt. 1: Removing redundant schema information**, see [topic](#opt-1-removing-redundant-schema-information)
2. Opt. 2: Removing redundant attribute values (e. g. shared string table)
3. Opt. 3: Handling sparse rows (most values are NULL) more efficiently
4. Opt. 4: Reducing space required for geometries (e. g. coordinates

Bold items have already been implemented.

The shown optimizations are ordered from *simple to implement* to *hard to implement* (not *really*
hard, however). That's also the intended order of implementation. Although some optimizations are
optional, all optimizations could be in effect at the same time. Then, each optimization
contributes his part to lower the space required for encoding a certain set of features.

In some cases, however, it may be useful to specify which optimizations shall be used for a
request. Several techniques are available to give a client the ability to specify the set of
SpatialJSON optimizations it is able or willing to use (e. g. parameter `format_options`,
additional `outputFormat` parameters). It's still not clear how this will be implemented and how
fine grained that will be.

## Opt. 1: Removing Redundant Schema Information

In traditional GeoJSON, every feature in a (simple feature) feature collection has its own schema
information. That is, every feature contains all its (not necessarily short) attribute names.
Except the geometry name, these names are used as the keys in the `"properties"` map:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "id": "areas.1",
      "geometry": {
        "type": "Point",
        "coordinates": [590529, 4914625]
      },
      "geometry_name": "the_geom",
      "properties": {
        "area_no": 12,
        "area_name": "Mainland",
        "area_description": "grassland",
        "area_cost_center": "0815"
      }
    },
    {
      "type": "Feature",
      "id": "areas.2",
      "geometry": {
        "type": "Point",
        "coordinates": [590215, 4913987]
      },
      "geometry_name": "the_geom",
      "properties": {
        "area_no": 17,
        "area_name": "South region",
        "area_description" : "meadow, pasture",
        "area_cost_center": "0812"
      }
    }
  ],
  "totalFeatures": 2,
  "numberMatched": 2,
  "numberReturned": 2,
  "timeStamp": "2022-10-17T08:12:45.248Z",
  "crs": {
    "type": "name",
    "properties": {
      "name": "urn:ogc:def:crs:EPSG::26713"
    }
  }
}
```

Since all features have the same schema information, SpatialJSON does not write attribute names for
every feature. Instead, a single `"schemaInformation"` property is added to the end of the
top-level `"FeatureCollection"` object:

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "id": "areas.1",
      "geometry": {
        "type": "Point",
        "coordinates": [590529, 4914625]
      },
      "properties": [12, "Mainland", "grassland", "0815"]
    },
    {
      "type": "Feature",
      "id": "areas.2",
      "geometry": {
        "type": "Point",
        "coordinates": [590215, 4913987]
      },
      "properties": [17, "South region", "meadow, pasture", "0812"]
    }
  ],
  "totalFeatures": 2,
  "numberMatched": 2,
  "numberReturned": 2,
  "timeStamp": "2022-10-17T08:14:36.521Z",
  "crs": {
    "type": "name",
    "properties": {
      "name": "urn:ogc:def:crs:EPSG::26713"
    }
  },
  "schemaInformation": {
    "propertyNames": ["area_no", "area_name", "area_description", "area_cost_center"],
    "geometryName": "the_geom"
  }
}
```

With SpatialJSON, each feature's `"properties"` map becomes an *ordered list* (array) whose index
corresponds to the `"propertyNames"` array that holds the attribute names in the new
`"schemaInformation"` object. Additionally, the repeated property `"geometry_name"` is replaced by
a single property named `"geometryName"` in the new schema information object.

### Evaluation

In the above example, without whitespaces and line breaks, savings in space are only about 5%. With
much more features savings could reach almost 27% (the ratio of the sizes of a GeoJSON and a
SpatialJSON feature object), that is, the size of the SpatialJSON response is only 73% of the size
of a traditional GeoJSON response. More savings are possible with more attributes per feature.
Savings basically depend on the ratio between schema information size and data size. In tests
requesting several thousands of simple features with 200+ columns/attributes savings up to 70% have
been achieved.

These savings drop to between \~50% and \~3% when a compressing content encoding method (like gzip,
deflate or brotli) is used on the wire. However, it's not all about transfer size. The smaller the
uncompressed JSON response, the lesser characters the client has to parse. Smaller uncompressed
responses are also much more memory-friendly on both the server and the client side.