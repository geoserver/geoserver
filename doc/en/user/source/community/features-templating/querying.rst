Backward Mapping
============================

When performing queries, using CQL filters, against layers that support a templated output, it will be possible to reference the template attributes in the CQL expressions. The plugin will take care of interpreting the CQL filter and translate it, when possible, to a data source native filter. For example, if that data source is a relational database, the CQL filter will be translated to one or multiple SQL queries that will be used to retrieve only the needed data.    

Consider the following GML output example:

.. code-block:: xml

 <?xml version="1.0" encoding="UTF-8"?>
 <wfs:FeatureCollection xmlns:wfs="http://www.opengis.net/wfs" xmlns:gml="http://www.opengis.net/gml" xmlns:st="http://www.stations.org/1.0" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" numberOfFeature="0" timeStamp="2021-07-16T08:38:50.735Z">
   <gml:featureMember>
      <st:MeteoStations gml:id="MeteoStationsFeature.7">
         <st:code>Station_BOL</st:code>
         <st:name>Bologna</st:name>
         <st:geometry>
            <gml:Point srsName="EPSG:4326" srsDimension="2">
               <gml:pos>11.34 44.5</gml:pos>
            </gml:Point>
         </st:geometry>
         <st:temperature>
            <st:Temperature>
               <st:time>2016-12-19T11:28:31.000Z</st:time>
               <st:value>35.0</st:value>
            </st:Temperature>
         </st:temperature>
         <st:temperature>
            <st:Temperature>
               <st:time>2016-12-19T11:28:55.000Z</st:time>
               <st:value>25.0</st:value>
            </st:Temperature>
         </st:temperature>
         <st:pressure>
            <st:Pressure>
               <st:time>2016-12-19T11:30:26.000Z</st:time>
               <st:value>1019.0</st:value>
            </st:Pressure>
         </st:pressure>
         <st:pressure>
            <st:Pressure>
               <st:time>2016-12-19T11:30:51.000Z</st:time>
               <st:value>1015.0</st:value>
            </st:Pressure>
         </st:pressure>
         <st:wind_speed>
            <st:Wind_speed>
               <st:time>2016-12-19T11:29:24.000Z</st:time>
               <st:value>80.0</st:value>
            </st:Wind_speed>
         </st:wind_speed>
      </st:MeteoStations>
   </gml:featureMember>
   <gml:featureMember>
      <st:MeteoStations gml:id="MeteoStationsFeature.13">
         <st:code>Station_ALS</st:code>
         <st:name>Alessandria</st:name>
         <st:geometry>
            <gml:Point srsName="EPSG:4326" srsDimension="2">
               <gml:pos>8.63 44.92</gml:pos>
            </gml:Point>
         </st:geometry>
         <st:temperature>
            <st:Temperature>
               <st:time>2016-12-19T11:26:40.000Z</st:time>
               <st:value>20.0</st:value>
            </st:Temperature>
         </st:temperature>
         <st:wind_speed>
            <st:Wind_speed>
               <st:time>2016-12-19T11:27:13.000Z</st:time>
               <st:value>155.0</st:value>
            </st:Wind_speed>
         </st:wind_speed>
      </st:MeteoStations>
   </gml:featureMember>
   <gml:featureMember>
      <st:MeteoStations gml:id="MeteoStationsFeature.21">
         <st:code>Station_ROV</st:code>
         <st:name>Rovereto</st:name>
         <st:geometry>
            <gml:Point srsName="EPSG:4326" srsDimension="2">
               <gml:pos>11.05 45.89</gml:pos>
            </gml:Point>
         </st:geometry>
      </st:MeteoStations>
   </gml:featureMember>
 </wfs:FeatureCollection>

The following are valid CQL_FILTERS

* :code:`st:name = 'Station_BOL'`.
* :code:`st:temperature.st:Temperature.st:value < 25`.

Given this underlying GML template:

.. code-block:: xml

  <gft:Template>
  <gft:Options>
    <gft:Namespaces xmlns:st="http://www.stations.org/1.0"/>
  </gft:Options>
 <st:MeteoStations gml:id="${@id}">
 <st:code>$${strConcat('Station_',st:code)}</st:code>
 <st:name>${st:common_name}</st:name>
 <st:geometry>${st:position}</st:geometry>
 <st:temperature gft:isCollection="true" gft:source="st:meteoObservations/st:MeteoObservationsFeature" gft:filter="xpath('st:meteoParameters/st:MeteoParametersFeature/st:param_name') = 'temperature'">
	<st:Temperature>
		<st:time>${st:time}</st:time>
		<st:value>${st:value}</st:value>
	</st:Temperature>
 </st:temperature>
 <st:pressure gft:isCollection="true" gft:source="st:meteoObservations/st:MeteoObservationsFeature"  gft:filter="xpath('st:meteoParameters/st:MeteoParametersFeature/st:param_name') = 'pressure'">
	<st:Pressure>
		<st:time>${st:time}</st:time>
		<st:value>${st:value}</st:value>
	</st:Pressure>
 </st:pressure>
 <st:wind_speed gft:isCollection="true" gft:source="st:meteoObservations/st:MeteoObservationsFeature"  gft:filter="xpath('st:meteoParameters/st:MeteoParametersFeature/st:param_name') = 'wind speed'">
	<st:Wind_speed>
		<st:time>${st:time}</st:time>
		<st:value>${st:value}</st:value>
	</st:Wind_speed>
 </st:wind_speed>
 </st:MeteoStations>
 </gft:Template>

The above cql_filter will be internally translated to:

* :code:`strConcat('Station_',st:code) = 'Station_BOL'`.
* :code:`st:meteoObservations/st:MeteoObservationsFeature/st:MeteoParametersFeature/st:value < 25 AND st:meteoObservations/st:MeteoObservationsFeature/st:MeteoParametersFeature/st:param_name = 'temperature'`.

As it is possible to see from the second example, if a template filter is defined for the value we want to filter by, the filter will be automatically included in the query.



Backwards mapping capability is available for all the output formats. Consider the following JSON-LD output example:

The following are example of valid CQL filters:

* gsml:GeologicUnit.description = 'some string value'
* name in ("MERCIA MUDSTONE", "UKNOWN")
* gsml:positionalAccuracy.valueArray1 = "100"

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
            "gsml:positionalAccuracy":{
                "value":"100",
                "valueArray": ["100","someStaticVal"]
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


The following are example of valid CQL filters:

* gsml:GeologicUnit.description = 'some string value'
* name in ("MERCIA MUDSTONE", "UKNOWN")
* gsml:positionalAccuracy.valueArray1 = "100"

As the last example shows, to refer to elements in arrays listing simple attributes, the index of the attribute is needed, starting from 1, in the form ``{attributeName}{index}``, as in ``features.gsml:positionalAccuracy.valueArray1.``
