Template Output Format
============================

JSON-LD
--------


In order to obtain a json-ld output format, request's media type must be specified as "application/ld+json".
From Geoserver UI it is possible to have a preview of the format by choosing JSON-LD format by the layer preview page.

.. figure:: images/json-ld_preview.png

   Dropdown menu in Layer preview page.



If OGC Feature API extension is enabled the output format is available from the collection page as well.


.. figure:: images/json-ld_wfs3.png

   Dropdown menu in OGC Feature API collection page.



The output, given the template files showed in the configuration section, will look, in  the json-ld case, like

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
            "@id": "mf2",
            "@type": [
                "Feature",
                "gsml:MappedFeature",
                "http://vocabulary.odm2.org/samplingfeaturetype/mappedFeature"
            ],
            "name": "MERCIA MUDSTONE GROUP",
            "gsml:positionalAccuracy": {
                "value": "100.0"
            },
            "gsml:GeologicUnit": {
                "@id": "gu.25678",
                "description": "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                "gsml:geologicUnitType": "urn:ogc:def:nil:OGC::unknown",
                "gsml:composition": [
                    {
                        "gsml:compositionPart": [
                            {
                                "gsml:role": {
                                    "value": "interbedded component",
                                    "@codeSpace": "urn:cgi:classi..."
                                },
                                "proportion": {
                                    "@dataType": "CGI_ValueProperty",
                                    "CGI_TermValue": {
                                        "@dataType": "CGI_TermValue",
                                        "value": {
                                            "value": "significant",
                                            "@codeSpace": "some:uri"
                                        }
                                    }
                                },
                                "lithology": [
                                    {
                                        "@id": "cc.3",
                                        "name": {
                                            "value": "name_cc_3",
                                            "@lang": "en"
                                        },
                                        "vocabulary": {
                                            "@href": "urn:ogc:def:nil:OGC::missing"
                                        }
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        "gsml:compositionPart": [
                            {
                                "gsml:role": {
                                    "value": "interbedded component",
                                    "@codeSpace": "urn:cgi:class..."
                                },
                                "proportion": {
                                    "@dataType": "CGI_ValueProperty",
                                    "CGI_TermValue": {
                                        "@dataType": "CGI_TermValue",
                                        "value": {
                                            "value": "minor",
                                            "@codeSpace": "some:uri"
                                        }
                                    }
                                },
                                "lithology": [
                                    {
                                        "@id": "cc.4",
                                        "name": {
                                            "value": "name_cc_4",
                                            "@lang": "en"
                                        },
                                        "vocabulary": {
                                            "@href": "urn:ogc:def:nil:OGC::missing"
                                        }
                                    }
                                ]
                            }
                        ]
                    }
                ],
                "geometry": {
                    "@type": "Polygon",
                    "wkt": "POLYGON ((52.5 -1.3, 52.6 -1.3, 52.6 -1.2,...))"
                }
            }
        }
    ]
    }

JSON-LD Validation
''''''''''''''''''

The plugin provides a validation for the json-ld output against the ``@context`` defined in the template. It is possible to require it by specifying a new query parameter in the request: ``validation=true``.
The validation takes advantage form the json-ld api and performes the following steps:

* the `expansion algorithm <https://www.w3.org/TR/json-ld11-api/#expansion-algorithm>`_ is executed against the json-ld output, expanding each features' attribute name to IRIs, removing those with no reference in the ``@context`` and the ``@context`` itself;

* the `compaction algorithm <https://www.w3.org/TR/json-ld11-api/#compaction-algorithm>`_ is then executed on the expansion result, putting back the ``@context`` and shortens to the terms the expanded attribute names as in the original output;

* finally the result of the compaction process is compared to the original json-ld and if some attributes are missing it means that they were not referenced in the ``@context``. An exception is thrown with a message pointing to the missing attributes.

GEOJSON
--------

The following is instead a GeoJSON output format, give the template showed in the configuration section.

.. code-block:: json

  {
   "type":"FeatureCollection",
   "features":[
      {
         "@id":"mf4",
         "@type":[
            "Feature",
            "gsml:MappedFeature",
            "http://vocabulary.odm2.org/samplingfeaturetype/mappedFeature"
         ],
         "name":"MURRADUC BASALT",
         "gsml:positionalAccuracy":{
            "type":"gsml:CGI_NumericValue",
            "value":120.0
         },
         "gsml:GeologicUnit":{
            "@id":"gu.25682",
            "description":"Olivine basalt",
            "gsml:geologicUnitType":"urn:ogc:def:nil:OGC::unknown",
            "gsml:composition":[
               {
                  "gsml:compositionPart":[
                     {
                        "gsml:role":{
                           "value":"RoleValue: interbedded component",
                           "@codeSpace":"urn:cgi:classifierScheme:Example:CompositionPartRole"
                        },
                        "proportion":{
                           "@dataType":"CGI_ValueProperty",
                           "CGI_TermValue":{
                              "@dataType":"CGI_TermValue",
                              "value":{
                                 "value":"significant",
                                 "@codeSpace":"some:uri"
                              }
                           }
                        },
                        "lithology":[
                           {
                              "@id":"cc.1",
                              "name":{
                                 "value":[
                                    "name_a",
                                    "name_b",
                                    "name_c"
                                 ],
                                 "@lang":"en"
                              },
                              "vocabulary":{
                                 "@href":"urn:ogc:def:nil:OGC::missing"
                              }
                           },
                           {
                              "@id":"cc.2",
                              "name":{
                                 "value":"name_2",
                                 "@lang":"en"
                              },
                              "vocabulary":{
                                 "@href":"urn:ogc:def:nil:OGC::missing"
                              }
                           }
                        ]
                     }
                  ]
               }
            ]
         },
         "geometry":{
            "type":"Polygon",
            "coordinates":[
               [
                  [
                     -1.3,
                     52.5
                  ],
                  [
                     -1.3,
                     52.6
                  ],
                  [
                     -1.2,
                     52.6
                  ],
                  [
                     -1.2,
                     52.5
                  ],
                  [
                     -1.3,
                     52.5
                  ]
               ]
            ]
         }
      }
   ]
 }

While by using the flat_output VendorOption the output will be:

.. code-block:: json

  {
   "type":"FeatureCollection",
   "features":[
      {
         "@id":"mf4",
         "geometry":{
            "type":"Polygon",
            "coordinates":[
               [
                  [
                     52.5,
                     -1.3
                  ],
                  [
                     52.6,
                     -1.3
                  ],
                  [
                     52.6,
                     -1.2
                  ],
                  [
                     52.5,
                     -1.2
                  ],
                  [
                     52.5,
                     -1.3
                  ]
               ]
            ]
         },
         "properties":{
            "name":"FeatureName: MURRADUC BASALT",
            "gsml:GeologicUnit_description":"Olivine basalt",
            "gsml:GeologicUnit_gsml:geologicUnitType":"urn:ogc:def:nil:OGC::unknown",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_gsml:role_value":"FeatureName: interbedded component",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_gsml:role_@codeSpace":"urn:cgi:classifierScheme:Example:CompositionPartRole",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_proportion_@dataType":"CGI_ValueProperty",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_proportion_CGI-TermValue_@dataType":"CGI_TermValue",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_proportion_CGI-TermValue_value":"significant",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_1_name_1":"name_a",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_1_name_2":"name_b",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_1_name_3":"name_c",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_1_vocabulary":"@href:urn:ogc:def:nil:OGC::missing",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_2_name":"name_2",
            "gsml:GeologicUnit_gsml:composition_gsml:compositionPart_lithology_2_vocabulary":"@href:urn:ogc:def:nil:OGC::missing"
         }
      }
   ]
 }