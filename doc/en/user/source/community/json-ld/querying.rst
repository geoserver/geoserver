JSON-LD Bacward Mapping
============================

JSON-LD Querying
---------------------


The module allows to query data passing a json-ld path in cql_filter in all wfs getFeature request. Json-ld path will be resolved against the json-ld template to pick up corresponding value that could be a static one or a dynamic one (in that case the value is represented by a cql or xpath filter).
A valid json-ld path comprises json-ld path field names separated by a ".". Having a json-ld template like the following


.. code-block:: json


  {
  "@hints": {
     "gsml": "urn:cgi:xmlns:CGI:GeoSciML:2.0",
     "om": "http://www.opengis.net/om/1.0",
     "sa": "http://www.opengis.net/sampling/2.0",
     "xlink": "http://www.w3.org/1999/xlink",
     "gml": "http://www.opengis.net/gml"
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
       "$source": "gsml:MappedFeature"
     },
     {
       "@id": "${@id}",
       "@type": [
         "Feature",
         "gsml:MappedFeature",
         "http://vocabulary.odm2.org/samplingfeaturetype/mappedFeature"
       ],
       "name": "${gml:name}",
       "gsml:positionalAccuracy": {
         "type": "gsml:CGI_NumericValue",
         "value": "${gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue}"
       },
       "gsml:GeologicUnit": {
         "$source": "gsml:specification/gsml:GeologicUnit",
         "@id": "${@id}",
         "description": "${gml:description}",
         "gsml:geologicUnitType": "urn:ogc:def:nil:OGC::unknown",
         "gsml:composition": [
           {
             "$source": "gsml:composition"
           },
           {
             "gsml:compositionPart": [
               {
                 "$source": "gsml:CompositionPart"
               },
               {
                 "gsml:role": {
                   "value": "${gsml:role}",
                   "@codeSpace": "urn:cgi:classifierScheme:Example:CompositionPartRole"
                 },
                 "proportion": {
                   "$source": "gsml:proportion",
                   "@dataType": "CGI_ValueProperty",
                   "CGI_TermValue": {
                     "@dataType": "CGI_TermValue",
                     "value": {
                       "value": "${gsml:CGI_TermValue}",
                       "@codeSpace": "some:uri"
                     }
                   }
                 },
                 "lithology": [
                   {
                     "$source": "gsml:lithology"
                   },
                   {
                     "@id": "${gsml:ControlledConcept/@id}",
                     "name": {
                       "value": "${gsml:ControlledConcept/gsml:name}",
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
         ]
       },
       "geometry": {
         "@type": "Polygon",
         "wkt": "$${toWKT(xpath('gsml:shape'))}"
       }
     }
   ]
  }


a valid cql_filter with a json-ld path could be :code:`features.gsml:GeologicUnit.description = 'some string value'`. The json-ld will be used to pick up the corresponding field value, namely the xpath :code:`gml:description` and the store will be queried for features having the value obtained from this xpath evaluation equals to the specified string.
