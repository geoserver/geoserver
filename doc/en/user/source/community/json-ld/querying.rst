JSON-LD Bacward Mapping
============================

JSON-LD Querying
---------------------


The module allows to query data passing a json-ld path in cql_filter in all wfs getFeature request. Json-ld path will be resolved against the json-ld template to pick up corresponding value that could be a static one or a dynamic one (in that case the value is represented by a cql or xpath filter).
A valid json-ld path comprises json-ld path field names separated by a ".". Having a json-ld template like the following


.. code-block:: json


 {  
 "@hints": {
    "st_gml31": "http://www.stations_gml31.org/1.0",
    "ms_gml31": "http://www.stations_gml31.org/1.0:measurements"
  }, 
  "@context": {
    "gsp": "http://www.opengis.net/ont/geosparql#",
    "sf": "http://www.opengis.net/ont/sf#",
    "schema": "https://schema.org/",
    "dc": "http://purl.org/dc/terms/",
    "Feature": "gsp:Feature",
    "FeatureCollection": "schema:Collection",
    "Point": "sf:Point",
    "wkt": "gsp:asWKT",
    "features": {
      "@container": "@set",
      "@id": "schema:hasPart"
    },
    "geometry": "sf:geometry",
    "description": "dc:description",
    "title": "dc:title",
    "name": "schema:name"
  },
  "type": "FeatureCollection",
  "features": [
    {
      "$source": "st_gml31:Station_gml31"
    },
    {
      "@id": "${@id}",
      "@type": [
        "Feature",
        "st_gml31:Station_gml31",
        "http://vocabulary.odm2.org/samplingfeaturetype/borehole"
      ],
      "name": "${st_gml31:name}",
      "geometry": {
        "@type": "Point",
        "wkt": "$${toWKT(xpath('st_gml31:location'))}"
      },
      "st_gml31:measurements": [
       {
          "$source": "st_gml31:Measurements"
        },
        {
          "name": "${st_gml31:measurements[1]}"
        },
        {
          "name": "${st_gml31:measurements[2]}"
        }
      ]
    }
  ]
 }


a valid cql_filter with a json-ld path could be :code:`features.st_gml31:measurements.name IS NOT NULL`. The json-ld will be used to pick up the corresponding field value, namely the xpath :code:`st_gml31:name` and the store will be queried for features having the value obtained from this xpath evaluation as not null.
