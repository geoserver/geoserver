 .. _spatialjson_schema:
 
Opt. 1: Removing Redundant Schema Information
=============================================

In traditional GeoJSON, every feature in a (simple feature) feature collection has its own schema
information. That is, every feature contains all its (not necessarily short) attribute names. Except
the geometry name, these names are used as the keys in the ``"properties"`` map:

.. code:: json

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

Since all features have the same schema information, SpatialJSON does not write attribute names for
every feature. Instead, a single ``"schemaInformation"`` property is added to the end of the
top-level ``"FeatureCollection"`` object:

.. code:: json

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

With SpatialJSON, each feature’s ``"properties"`` map becomes an *ordered list* (array) whose index
corresponds to the ``"propertyNames"`` array that holds the attribute names in the new 
``"schemaInformation"`` object. Additionally, the repeated property ``"geometry_name"`` is replaced
by a single property named ``"geometryName"`` in the new schema information object.

Evaluation
----------

In the above example, without whitespaces and line breaks, savings in space are only about 5%. With
much more features savings could reach almost 27% (the ratio of the sizes of a GeoJSON and a
SpatialJSON feature object), that is, the size of the SpatialJSON response is only 73% of the size
of a traditional GeoJSON response. More savings are possible with more attributes per feature.
Savings basically depend on the ratio between schema information size and data size. In tests
requesting several thousands of simple features with 200+ columns/attributes savings up to 70% have
been achieved.

These savings drop to between ~50% and ~3% when a compressing content encoding method (like gzip,
deflate or brotli) is used on the wire. However, it’s not all about transfer size. The smaller the
uncompressed JSON response, the lesser characters the client has to parse. Smaller uncompressed
responses are also much more memory-friendly on both the server and the client side.
