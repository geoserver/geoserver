Template Configuration
======================
 
Producing the template file
---------------------------

The template file, operate as a mapping level over the stream of features received by a store, transforming them in the desired output. 
The file has to be managed directly through file system editing, without any UI or REST API. In order to associate it with a given feature type, it has to be placed in FeatureType folder in the GeoServer data directory named as json-ld-template.json,
or as geojson-template.json e.g. :code:`workspace/store/featuretype/json-ld-template.json`.
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


While this is an example for a GeoJSON template

.. code-block:: json

   {
   "type":"FeatureCollection",
   "features":[
      {
         "$source":"gsml:MappedFeature"
      },
      {
         "@id":"${@id}",
         "@type":[
            "Feature",
            "gsml:MappedFeature",
            "http://vocabulary.odm2.org/samplingfeaturetype/mappedFeature"
         ],
         "name":"$${strConcat('FeatureName: ', xpath('gml:name'))}",
         "gsml:positionalAccuracy":{
            "type":"gsml:CGI_NumericValue",
            "value":"${gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue}"
         },
         "gsml:GeologicUnit":{
            "$source":"gsml:specification/gsml:GeologicUnit",
            "@id":"${@id}",
            "description":"${gml:description}",
            "gsml:geologicUnitType":"urn:ogc:def:nil:OGC::unknown",
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
                           "value":"$${strConcat('FeatureName: ', xpath('gsml:role'))}",
                           "@codeSpace":"urn:cgi:classifierScheme:Example:CompositionPartRole"
                        },
                        "proportion":{
                           "$source":"gsml:proportion",
                           "@dataType":"CGI_ValueProperty",
                           "CGI_TermValue":{
                              "@dataType":"CGI_TermValue",
                              "value":{
                                 "value":"${gsml:CGI_TermValue}",
                                 "@codeSpace":"some:uri"
                              }
                           }
                        },
                        "lithology":[
                           {
                              "$source":"gsml:lithology"
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
                  ]
               }
            ]
         },
         "geometry":"${gsml:shape}"
      }
   ]
 }


The content of the output depends on specified properties in the template file, in a way that follows the below rules:

* xpath property interpolation can be invoked using a :code:`${xpath}` syntax;
* in case complex operation are needed a CQL expression can be used throught a :code:`$${cql}` syntax (all CQL functions are supported);
* properties without directives are reproduced in the output as-is;
* a :code:`"$source":"xpath"` attribute can be added as the first element of an array or of an object;
* if a :code:`"$source": "xpath"` attribute is present, it will act as a context against which all xpath expression will be evaluated. In the case of an array it will be use to iterate over a collection of element; if source evaluates to null the entire object/array will be skipped;
* a :code:`../` syntax in an xpath means that xpath evaluation will be relative to the previous :code:`$source`. Give the above template file, the xpath :code:`"../gsml:shape"` will be evaluate not against the corresponding :code:`"$source": "gsml:specification/gsml:GeologicUnit"`, but against the parent one :code:`"$source": "gsml:MappedFeature"`.

.. warning:: the :code:`xpath('some xpath)` cql function is meant to be used in the scope of this plugin. For general usage please refers to the `property function <https://docs.geotools.org/latest/userguide/library/main/function_list.html#property-propertyname-returns-propertyvalue>`_


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


Inspire GeoJSON Output
----------------------

In order to provide a GeoJSON output encoded following INSPIRE rule for `alternative feature GeoJSON encoding <https://github.com/INSPIRE-MIF/2017.2/blob/master/GeoJSON/ads/simple-addresses.md>`_ (`see also <https://github.com/INSPIRE-MIF/2017.2/blob/master/GeoJSON/efs/simple-environmental-monitoring-facilities.md>`_), it is possible to provide a VendorOption in the template file by adding the following attribute :code:`"$VendorOptions": "flat_output:true"`.
Along with the :code:`flat_output` vendor option it is possible to specify a  :code:`separator` option, to customize the attribute name separator eg :code:`"$VendorOptions": "flat_output:true;separator:."`. Default is :code:`_`.
Below an example configuration file for a Complex Feature type:

.. code-block:: json


  {
   "$VendorOptions":"flat_output:true",
   "type":"FeatureCollection",
   "features":[
      {
         "$source":"gsml:MappedFeature"
      },
      {
         "@id":"${@id}",
         "geometry":"${gsml:shape}",
         "properties":{
            "name":"$${strConcat('FeatureName: ', xpath('gml:name'))}",
            "gsml:GeologicUnit":{
               "$source":"gsml:specification/gsml:GeologicUnit",
               "description":"${gml:description}",
               "gsml:geologicUnitType":"urn:ogc:def:nil:OGC::unknown",
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
                           "gsml:role_value":"$${strConcat('FeatureName: ', xpath('gsml:role'))}",
                           "gsml:role_codeSpace":"urn:cgi:classifierScheme:Example:CompositionPartRole",
                           "proportion":{
                              "$source":"gsml:proportion",
                              "@dataType":"CGI_ValueProperty",
                              "CGI-TermValue_@dataType":"CGI_TermValue",
                              "CGI-TermValue_value":"${gsml:CGI_TermValue}"
                           },
                           "lithology":[
                              {
                                 "$source":"gsml:lithology"
                              },
                              {
                                 "name":"${gsml:ControlledConcept/gsml:name}",
                                 "vocabulary":"@href:urn:ogc:def:nil:OGC::missing"
                              }
                           ]
                        }
                     ]
                  }
               ]
            }
         }
      }
   ]
 }


Given the above configuration file, the plugin will act in the following way:

 * the encoding of nested arrays and objects will be skipped, by encoding only their attributes.
 * Arrays' and objects' attribute names will be concatenated with the ones of their json attributes.
 * The final output will have a flat list of attributes with names produced by the concatenation.

An example output, give this configuration file, can be seen in the output section.

Environment parametrization
---------------------------

A template configuration can also be manipulated on the fly, replacing existing attributes, attributes' names and sources using the :code:`env` parameter. 
To achieve this the attribute name, the attribute, or the source should be replaced by the env function in the following way :code:`$${env('nameOfTheEnvParameter','defaultValue')}`. 
If in the request it is specified an env query parameter :code:`env='nameOfTheEnvParameter':'newValue'`, the default value will be replaced in the final output with the one specified in the request.

The functionality allows also to manipulate dynamically filters and expression. For example it is possible to change Filter arguments: :code:`"$filter":"xpath('gsml:name') = env('nameOfTheEnvParameter','defaultValue')`.

Xpaths can be manipulated as well to be totally or partially replaced: :code:`$${xpath(env('xpath','gsml:ControlledConcept/gsml:name')}` or :code:`$${xpath(strConcat('env('gsml:ControlledConcept',xpath','/gsml:name')))}`.

