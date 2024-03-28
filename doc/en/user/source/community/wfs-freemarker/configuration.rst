.. _community_wfsfreemarker_config:

WFS FreeMarker Extension configuration
======================================

Template Lookup
```````````````

GeoServer looks up templates in three different places, allowing for various levels of customization. For example given the ``content.ftl`` template used to generate WMS GetFeatureInfo content:

* Look into ``GEOSERVER_DATA_DIR/workspaces/<workspace>/<datastore>/<featuretype>/content.ftl`` to see if there is a feature type specific template
* Look into ``GEOSERVER_DATA_DIR/workspaces/<workspace>/<datastore>/content.ftl`` to see if there is a store specific template
* Look into ``GEOSERVER_DATA_DIR/workspaces/<workspace>/content.ftl`` to see if there is a workspace specific template
* Look into ``GEOSERVER_DATA_DIR/workspaces/content.ftl`` looking for a global override
* Look into ``GEOSERVER_DATA_DIR/templates/content.ftl`` looking for a global override
* Look into the GeoServer classpath and load the default template

Each templated output format tutorial should provide you with the template names, and state whether the templates can be type specific, or not.  If you can't find the source files for the default template, look in the service jar of the geoserver distribution (for example, wms-x.y.z.jar). You just have to unzip it, and you'll find the xxx.ftl files GeoServer is using as the default templates.

Example Configuration on a Vectorial Layer
``````````````````````````````````````````

The WFS GetFeature can generate output in various formats: GML, GeoJSON, ... and, through this extention, also HTML.

WFS Templating is concerned with the HTML one.

Assume we have a vectorial layer named :guilabel:`geosolutions:bplandmarks`

#. Go to the Layer preview to show :guilabel:`geosolutions:bplandmarks` layer.

#. Search for the HTML format from the :guilabel:`All Formats` selectbox, under the WFS ones.

    .. figure:: images/info1.png

#. In order to configure a custom template of the GetFeature results create three files ``.ftl`` in ``$geoserver_data/workspaces/geosolutions`` directory named:

    .. code::
  
       - header.ftl
       - content.ftl
       - footer.ftl
  
    .. note::
  
       The Template is managed using Freemarker. This is a simple yet powerful template engine that GeoServer uses whenever developers allowed user customization of textual outputs. In particular, at the time of writing, it’s used to allow customization of GetFeatureInfo, GeoRSS and KML outputs.
  
    .. note::
  
       Splitting the template in three files allows the administrator to keep a consistent styling for the GetFeatureInfo result, but use different templates for different workspaces or different layers. This is done by providing a master header.ftl and footer.ftl file, but specify a different content.ftl for each layer.
  
#. In header.ftl file enter the following HTML:

    .. code::
  
       <#--
       Header section of the GetFeatureInfo HTML output. Should have the <head> section, and
       a starter of the <body>. It is advised that eventual CSS uses a special class for featureInfo,
       since the generated HTML may blend with another page changing its aspect when using generic classes
       like td, tr, and so on.
       -->
       <html>
               <head>
                       <title>Geoserver GetFeatureInfo output</title>
               </head>
               <style type="text/css">
                       table.featureInfo, table.featureInfo td, table.featureInfo th {
                               border:1px solid #ddd;
                               border-collapse:collapse;
                               margin:0;
                               padding:0;
                               font-size: 90%;
                               padding:.2em .1em;
                       }
       
                       table.featureInfo th{
                               padding:.2em .2em;
                               text-transform:uppercase;
                               font-weight:bold;
                               background:#eee;
                       }
       
                       table.featureInfo td{
                               background:#fff;
                       }
       
                       table.featureInfo tr.odd td{
                               background:#eee;
                       }
       
                       table.featureInfo caption{
                               text-align:left;
                               font-size:100%;
                               font-weight:bold;
                               text-transform:uppercase;
                               padding:.2em .2em;
                       }
               </style>
               <body>

#. In content.ftl file enter the following HMTL:

    .. code::
  
       <ul>
       <#list features as feature>
               <li><b>Type: ${type.name}</b> (id: <em>${feature.fid}</em>):
               <ul>
               <#list feature.attributes as attribute>
                       <#if !attribute.isGeometry>
                               <li>${attribute.name}: ${attribute.value}</li>
                       </#if>
               </#list>
               </ul>
               </li>
       </#list>
       </ul>

#. In footer.ftl file enter the following HMTL:

    .. code::
  
       <#--
       Footer section of the GetFeatureInfo HTML output. Should close the body and the html tag.
       -->
               </body>
       </html>

#. Refresh the WFS GetFeature HTML output

    .. figure:: images/info2.png