.. _tutorials_getfeatureinfo_geojson:

GeoJSON output format
======================

The default GeoJSON output uses the WFS GeoJSON  encoding mechanism, producing a fixed output, it is however possible to customize the output using FreeMarker templates.
GeoServer will lookup for json templates following the same rules defined for the hmtl output, but the template files have to be named appending ``_json`` to the usual name, as below:

* ``header_json.ftl``
* ``content_json.ftl``
* ``footer_json.ftl``

Moreover, unlike the html case, all three template files must be provided.
In case of a multi-layer request GeoServer will act in the following way:

* content template will be searched up following the usual rules;
* since there are no default templates for GeoJSON, header and footer will be looked up in the ``templates`` directory;
* features with no content template will be encoded with the normal GeoJSON encoding, along with the customized ones.


Follow examples of json template for each type.

The *header json template*::

 {
  "header":"this is the header",
  "type":"FeatureCollection",
  "features":[
	

The *footer json template*::

  ],
  "footer" : "this is the footer"
 }


The *content json template*::

 <#list features as feature>
  {
   "content" : "this is the content",
   "type": "Feature",
   "id" : "${feature.fid}",
   "properties": {
   <#list feature.attributes as attribute>
   <#if !attribute.isGeometry>
   "${attribute.name}": "${attribute.value}"
   </#if>
   <#if attribute_has_next && !attribute.isGeometry>
   ,
   </#if>
   </#list>
   }
 }
  <#if feature_has_next>
  ,
  </#if>
 </#list>
 


Placing the above templates in the directory of layer tiger_roads and issuing this request, ::

  http://localhost:8080/geoserver/tiger/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetFeatureInfo&FORMAT=application/json&TRANSPARENT=true&QUERY_LAYERS=tiger:tiger_roads&LAYERS=tiger:tiger_roads&exceptions=application/vnd.ogc.se_inimage&INFO_FORMAT=application/json&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG:4326&STYLES=&WIDTH=101&HEIGHT=101&BBOX=-73.96894311918004,40.78191518783569,-73.96460866941197,40.78624963760376

the output will be:

.. code-block:: json


 {
   "header":"this is the header",
   "type":"FeatureCollection",
   "features":[
      {
         "content":"this is the content",
         "type":"Feature",
         "id":"tiger_roads.7752",
         "properties":{
            "CFCC":"A41",
            "NAME":"85th St Transverse"
         }
      }
   ],
   "footer":"this is the footer"
 }


While taking care of moving header_json.ftl and footer_json.ftl into the templates directory and performing the following request against the layer group tiger-ny ::
 
  http://localhost:8080/geoserver/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetFeatureInfo&FORMAT=application/json&TRANSPARENT=true&QUERY_LAYERS=tiger-ny&LAYERS=tiger-ny&exceptions=application/vnd.ogc.se_inimage&INFO_FORMAT=application/json&FEATURE_COUNT=50&X=50&Y=50&SRS=EPSG:4326&STYLES=&WIDTH=101&HEIGHT=101&BBOX=-74.01161170018896,40.70833468424098,-74.00944447530493,40.710501909125014


the output will be:

.. code-block:: json


 {
   "header":"this is the header",
   "type":"FeatureCollection",
   "features":[
      {
         "type":"Feature",
         "id":"giant_polygon.1",
         "geometry":{
            "type":"MultiPolygon",
            "coordinates":[
               [
                  [
                     [
                        -180,
                        -90
                     ],
                     [
                        -180,
                        90
                     ],
                     [
                        180,
                        90
                     ],
                     [
                        180,
                        -90
                     ],
                     [
                        -180,
                        -90
                     ]
                  ]
               ]
            ]
         },
         "properties":{
            "@featureType":"giant_polygon",
            "the_geom":{
               "type":"MultiPolygon",
               "coordinates":[
                  [
                     [
                        [
                           -180,
                           -90
                        ],
                        [
                           -180,
                           90
                        ],
                        [
                           180,
                           90
                        ],
                        [
                           180,
                           -90
                        ],
                        [
                           -180,
                           -90
                        ]
                     ]
                  ]
               ]
            }
         }
      },
      {
         "content":"this is the content",
         "type":"Feature",
         "id":"tiger_roads.7672",
         "properties":{
            "CFCC":"A41",
            "NAME":"Broadway"
         }
      },
      {
         "type":"Feature",
         "id":"poi.3",
         "geometry":{
            "type":"Point",
            "coordinates":[
               -74.01053,
               40.709387
            ]
         },
         "properties":{
            "@featureType":"poi",
            "the_geom":{
               "type":"Point",
               "coordinates":[
                  -74.01053,
                  40.709387
               ]
            },
            "NAME":"art",
            "THUMBNAIL":"pics/22037856-Ti.jpg",
            "MAINPAGE":"pics/22037856-L.jpg"
         }
      }
   ],
   "footer":"this is the footer"
 }

As it is possible to see the json output comprise a mix of the output mediated by a content_json.ftl for the tiger_roads feature, and the normal output for the other features, while header and footer have been kept respectively at the top and at the bottom.
