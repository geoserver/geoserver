JSON-LD Configuration
=====================
 
Producing the template file
---------------------------

JSON-LD template file, operate as a mapping level over the stream of features received by a store, transforming them in the desired output. 
The template file will be managed directly through file system editing, without any UI or REST API. In order to associate it with a given feature type, it has to be placed in FeatureType folder in the GeoServer data directory named as json-ld-template.json, e.g. :code:`workspace/store/featuretype/json-ld-template.json`.
If the client asks json-ld output format  for a feature type that does not have a json-ld template file, an error will be returned.
This is an example of a json-ld configuration file 

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




The content of the json-ld output depends on specified properties in json-ld template file, in a way that follows below rules:

* xpath property interpolation can be invoked using a :code:`${xpath}` syntax;
* in case complex operation are needed a CQL expression can be used throught a :code:`$${cql}` syntax (all CQL functions are supported);
* properties without directives are reproduced in the output as-is;
* a :code:`"$source":"xpath"` attribute can be added as the first element of an array or of an object;
* if a :code:`"$source": "xpath"` attribute is present, it will act as a context against which all xpath expression will be evaluated. In the case of an array it will be use to iterate over a collection of element; if source evaluates to null the entire object/array will be skipped;
* a :code:`../` syntax in an xpath means that xpath evaluation will be relative to the previous :code:`$source`. Give the above template file, the xpath :code:`"../gsml:shape"` will be evaluate not against the corresponding :code:`"$source": "gsml:specification/gsml:GeologicUnit"`, but against the parent one :code:`"$source": "gsml:MappedFeature"`.
