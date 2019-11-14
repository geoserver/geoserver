JSON-LD Configuration
=====================
 
Producing the template file
---------------------------

JSON-LD template file, operate as a mapping level over the stream of features received by a store, transforming them in the desired output.
This is an example of a json-ld configuration file ..code-block: json::

  {   
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
        {"$source": "st_gml31:measurements"},
        {
          "name": "${../st_gml31:measurements[1]}",
          "stillThePoint": {
            "@type": "Point",
            "wkt": "$${toWKT(xpath('../st_gml31:location'))}"
          }
        },
        {
          "name": "${../st_gml31:measurements[2]}",
          "stillThePoint": {
            "@type": "Point",
            "wkt": "$${toWKT(xpath('../st_gml31:location'))}"
          }
        }
      ]
    }
  ]
 }


The content of the json-ld output depends on specified properties in json-ld template file, in a way that follows below rules:

* xpath property interpolation can be invoked using a ``${xpath}`` syntax;
* in case complex operation are needed a CQL expression can be used throught a ``$${cql}`` syntax;
* properties without directives are reproduced in the output as-is;
* a ``"$source":"xpath"`` attribute can be added as the first element of an array or of an object;
* if a ``"$source": "xpath"`` attribute is present, it will act as a context against which all xpath expression will be evaluated (if source evaluates to null the entire object/array will be skipped);
* a ``../`` syntax in an xpath means that xpath evaluation will be relative to the previous ``$source``. Give the above template file, the xpath ``"../st_gml31:location"`` will be evaluate not against the corresponding ``"$source":"st_gml31:Station_gml31"``, but against the parent one ``"st_gml31:Station_gml31"``.






 
