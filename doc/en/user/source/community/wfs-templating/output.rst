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
            "gsml:GeologicUnit.description":"Olivine basalt",
            "gsml:GeologicUnit.gsml:geologicUnitType":"urn:ogc:def:nil:OGC::unknown",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.gsml:role.value":"FeatureName: interbedded component",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.gsml:role.@codeSpace":"urn:cgi:classifierScheme:Example:CompositionPartRole",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.proportion.@dataType":"CGI_ValueProperty",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.proportion.CGI_TermValue.@dataType":"CGI_TermValue",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.proportion.CGI_TermValue.value":"significant",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_1.name_1":"name_a",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_1.name_2":"name_b",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_1.name_3":"name_c",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_1.vocabulary":"@href:urn:ogc:def:nil:OGC::missing",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_2.name":"name_2",
            "gsml:GeologicUnit.gsml:composition.gsml:compositionPart.lithology_2.vocabulary":"@href:urn:ogc:def:nil:OGC::missing"
         }
      }
   ]
 }