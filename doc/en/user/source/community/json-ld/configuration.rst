JSON-LD Configuration
=====================
 
Producing the template file
---------------------------

JSON-LD template file, operate as a mapping level over the stream of features received by a store, transforming them in the desired output. 
The template file will be managed directly through file system editing, without any UI or REST API. In order to associate it with a given feature type, it has to be placed in FeatureType folder in the GeoServer data directory named as the FeatureType, e.g. :code:`workspace/store/featuretype/featuretype.json`. In case of complex feature, a json object named @hints listing namespaces has to be provided.
If the client asks json-ld output format  for a feature type that does not have a json-ld template file, an error will be returned.
This is an example of a json-ld configuration file 

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
          "name": "${st_gml31:measurements[1]}",
          "stillThePoint": {
            "@type": "Point",
            "wkt": "$${toWKT(xpath('st_gml31:location'))}"
          }
        },
        {
          "name": "${st_gml31:measurements[2]}",
          "stillThePoint": {
            "@type": "Point",
            "wkt": "$${toWKT(xpath('st_gml31:location'))}"
          }
        }
      ]
    }
  ]
 }


The content of the json-ld output depends on specified properties in json-ld template file, in a way that follows below rules:

* xpath property interpolation can be invoked using a :code:`${xpath}` syntax;
* in case complex operation are needed a CQL expression can be used throught a :code:`$${cql}` syntax (all CQL functions are supported);
* properties without directives are reproduced in the output as-is;
* a :code:`"$source":"xpath"` attribute can be added as the first element of an array or of an object;
* if a :code:`"$source": "xpath"` attribute is present, it will act as a context against which all xpath expression will be evaluated. In the case of an array it will be use to iterate over a collection of element; if source evaluates to null the entire object/array will be skipped;
* a :code:``../`` syntax in an xpath means that xpath evaluation will be relative to the previous :code:`$source`. Give the above template file, the xpath :code:`"../st_gml31:location"` will be evaluate not against the corresponding :code:`"$source":"st_gml31:Station_gml31"`, but against the parent one :code:`"st_gml31:Station_gml31"`.
