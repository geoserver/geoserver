# Slim GeoJSON WFS output format extension

This module adds the Slim GeoJSON WFS output format. That format is a more compact and memory-friendly variant of traditional GeoJSON, which saves space by removing redundant schema information for simple feature results. For complex features there are no differences compared to traditional GeoJSON output.

This module adds these additional WFS output formats for requesting simple features in Slim GeoJSON format:

- `application/json; subtype=geojson/slim`  
   for Slim GeoJSON
- `text/javascript; subtype=geojson/slim`  
   for Slim GeoJSON in JSONP requests

In traditional GeoJSON, every feature in a (simple feature) feature collection has its own schema information. That is, every feature contains all its (not necessarily short) attribute names. Except the geometry name, these are used as the keys in the `"properties"` map:

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
  "timeStamp": "2022-10-03T08:44:45.118Z",
  "crs": {
    "type": "name",
    "properties": {
      "name": "urn:ogc:def:crs:EPSG::26713"
    }
  }
}
```

Since all features have the same schema information, we do not have to write the attribute names for every feature. Instead, we add a single `"schema"` property to the end of the top-level `"FeatureCollection"` object:

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
  "timeStamp": "2022-10-03T08:44:45.118Z",
  "crs": {
    "type": "name",
    "properties": {
      "name": "urn:ogc:def:crs:EPSG::26713"
    }
  },
  "schema": {
    "properties": ["area_no", "area_name", "area_description", "area_cost_center"],
    "geometry_name": "the_geom"
  }
}
```

With Slim GeoJSON, each feature's `"properties"` map becomes an _ordered list_ (array) whose index corresponds to the `"properties"` array that holds the attribute names in the new `"schema"` object. The `"geometry_name"` becomes an additional property of the new schema object.

In the above example, without whitespaces and line breaks, savings are only about 5%. With much more features savings could reach almost 27% (the ratio of the sizes of two feature objects), that is, the size of the Slim GeoJSON is only 73% of the size of traditional GeoJSON. More savings are possible with more attributes per feature. Savings basically depend on the ratio between schema information size and data size. In tests requesting several thousands of simple features with 200+ columns/attributes savings up to 70% have been achieved.

These savings drop to between \~50% and \~97% when a compressing content encoding method (like gzip, deflate or brotli) is used on the wire. However, it's not all about transfer size. The smaller the uncompressed JSON response, the lesser the client's JSON parser has to process. Smaller uncompressed responses are also much more memory-friendly on both the server and the client side.