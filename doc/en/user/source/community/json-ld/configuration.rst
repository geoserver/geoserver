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


Filtering Support
------------------

In order to have a more fined grained control over the output it is possible to specify a filter at the array, object and attribute level.
Assuming to have a template file like the above, valid filters could be the followings:

array 

.. code-block:: json

 {
   "lithology":[
      {
         "$source":"gsml:lithology",
         "$filter":"xpath('gsml:ControlledConcept/gsml:name') = 'name_2'"
      },
      {
         "@id":"${gsml:ControlledConcept/@id}",
         "name":{
            "value":"${gsml:ControlledConcept/gsml:name}",
            "@lang":"en"
         },
         "vocabulary":{
            "@href":"urn:ogc:def:nil:OGC::missing"
         }
      }
   ]
 }


object 

.. code-block:: json

 {
   "gsml:GeologicUnit":{
      "$source":"gsml:specification/gsml:GeologicUnit",
      "$filter":"xpath('gml:description') = 'Olivine basalt'",
      "@id":"${@id}",
      "description":"${gml:description}",
      "gsml:geologicUnitType":"urn:ogc:def:nil:OGC::unknown",
      "gsml:composition":"..."
   }
 }



attribute (dynamic) 

.. code-block:: json

  {
  "gsml:GeologicUnit": {
        "$source": "gsml:specification/gsml:GeologicUnit",
        "@id": "${@id}",
        "description": "$filter{xpath('gml:description')='Olivine basalt'},${gml:description}",
        "gsml:geologicUnitType": "urn:ogc:def:nil:OGC::unknown",
        "gsml:composition": "..."
    }
  }


attribute (static) 

.. code-block:: json

   {
   "gsml:composition":[
      {
         "$source":"gsml:composition"
      },
      {
         "gsml:compositionPart":[
            {
               "$source":"gsml:CompositionPart"
            },
            {
               "gsml:role":{
                  "value":"${gsml:role}",
                  "@codeSpace":"$filter{xpath('../../gml:description')='Olivine basalt'},urn:cgi:classifierScheme:Example:CompositionPartRole"
               }
            }
         ]
      }
   ]
 }



In the array and object case the filter sintax expected a :code:`"$filter"` key followed by an attribute with the filter to evaluate. In the attribute case, instead, the filter is being specified inside the value as :code:`"$filter{...}"`, followed by  the cql expression, or by the static content, with a comma separating the two.
The evaluation of a filter is handled by the module in the following way:

* if a :code:`"$filter": "cql"` attribute is present after the :code:`"$source"` attribute in an array or an object:
  
  * in the array case, each array element will be included in the output only if the condition in the filter is matched, otherwise it will be skipped;
  
  * in the object case, the entire object will be included in the output only if the condition in the filter is matched, otherwise the object will be skipped;

* if a :code:`$filter{cql}` is present inside an attribute value before the expression or the static content, separated by it from a :code:`,`:
  
  * in case of an expression attribute, the result of the expression will be included in the output if the filter condition is true;
  
  * in case of a static content attribute, the static content will be included in the output if the filter condition is true.
  
  * in case the expression is not matched, the content, static or dynamic, will not be set, resulting in the attribute being skipped.