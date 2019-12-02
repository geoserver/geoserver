JSON-LD Output Format
============================

JSON-LD Output Format
---------------------


In order to obtain a json-ld output format, request's media type must be specified as "application/ld+json".
From Geoserver UI it is possible to have a preview of the format by choosing JSON-LD format by the layer preview page.

.. figure:: images/json-ld_preview.png

   Dropdown menu in Layer preview page.



If OGC Feature API extension is enabled the output format is available from wfs3 collection page as well.


.. figure:: images/json-ld_wfs3.png

   Dropdown menu in wfs3 collection page.



The output, given the template file showed in the configuration section, will look like 

.. code-block:: json

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
            "@id": "st.1",
            "@type": [
                "Feature",
                "st_gml31:Station_gml31",
                "http://vocabulary.odm2.org/samplingfeaturetype/borehole"
            ],
            "name": "station1",
            "geometry": {
                "@type": "Point",
                "wkt": "POINT (1 -1)"
            },
            "st_gml31:measurements": [
                {
                    "name": "temperature",
                    "stillThePoint": {
                        "@type": "Point",
                        "wkt": "POINT (1 -1)"
                    }
                },
                {
                    "name": "wind",
                    "stillThePoint": {
                        "@type": "Point",
                        "wkt": "POINT (1 -1)"
                    }
                }
            ]
        }
    ]
 }   


