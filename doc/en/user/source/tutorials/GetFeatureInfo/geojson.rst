GeoJSON output format
======================

The default GeoJSON output doesn't rely on free marker templates to be produced, nonetheless it is possible to customize the output using them.
GeoServer will lookup for json templates following the same rules defined for the hmtl output. The only difference regards the file names of the templates which have to be named appending ``_json`` to the name, as below:

* ``header_json.ftl``
* ``content_json.ftl``
* ``footer_json.ftl``


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
    "type": "${type.name}"
    <#list feature.attributes as attribute>
    , "${attribute.name}": "${attribute.value}"
    </#list>
  }
 </#list>

