.. _app-schema.wms-support:

WMS Support
===========

App-schema supports WMS requests as well as WFS requests. 
This page provides some useful examples for configuring the WMS service to work with complex features.

Note that the rendering performance of WMS can be significantly slower when using app-schema data stores. We strongly recommend employing :ref:`app-schema.joining` when using WMS with feature chaining, which can make response time for large data requests several orders of magnitude faster.

Configuration
-------------

For WMS to be applicable to complex feature data, it is necessary that the complex feature types are recognised by GeoServer as layers. This must be configured by adding an extra configuration file named 'layer.xml' in the data directory of each feature type that we want to use as a WMS layer.

This will expand the structure of the ``workspaces`` folder in the GeoServer data directory as follows (``workspaces``) (see  :ref:`app-schema.configuration`): ::

    workspaces
        - gsml
            - SomeDataStore
                - SomeFeatureType
                    - featuretype.xml
		    - layer.xml
                - datastore.xml
                - SomeFeatureType-mapping-file.xml


The file layer.xml must have the following contents: ::

      <layer>
	<id>[mylayerid]</id>
	<name>[mylayername]</name>
	<path>/</path>
	<type>VECTOR</type>
	<defaultStyle>
	      <name>[mydefaultstyle]</name>
	</defaultStyle>
	<resource class="featureType">
	      <id>[myfeaturetypeid]</id>
	</resource>
	<enabled>true</enabled>
	<attribution>
	      <logoWidth>0</logoWidth>
	      <logoHeight>0</logoHeight>
	</attribution>
      </layer> 

Replace the fields in between brackets with the following values:

* **[mylayerid]** must be a custom id for the layer.
* **[mylayername]** must be a custom name for the layer.
* **[mydefaultstyle]** the default style used for this layer (when a style is not specified in the wms request). The style must exist in the GeoServer configuration.
* **[myfeaturetypeid]** is the id of the feature type. This *must* the same as the id specified in the file featuretype.xml of the same directory.



GetMap
-------

Read :ref:`wms_getmap` for general information on the GetMap request.
Read :ref:`styling` for general information on how to style WMS maps with SLD files.
When styling complex features, you can use XPaths to specify nested properties in your filters, as explained in :ref:`app-schema.filtering-nested`. However,  in WMS styling filters X-paths do not support handling referenced features (see  :ref:`app-schema.feature-chaining-by-reference`) as if they were actual nested features (because the filters are applied after building the features rather than before.)
The prefix/namespace context that is used in the XPath expression is defined locally in the XML tags of the style file.
This is an example of a Style file for complex features:

.. code-block:: xml 
   :linenos: 

   <?xml version="1.0" encoding="UTF-8"?>
   <StyledLayerDescriptor version="1.0.0" 
       xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
       xmlns:ogc="http://www.opengis.net/ogc" 
       xmlns:xlink="http://www.w3.org/1999/xlink" 
       xmlns:gml="http://www.opengis.net/gml" 
       xmlns:gsml="urn:cgi:xmlns:CGI:GeoSciML:2.0"
       xmlns:sld="http://www.opengis.net/sld"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <sld:NamedLayer>
     <sld:Name>geology-lithology</sld:Name>
     <sld:UserStyle>
       <sld:Name>geology-lithology</sld:Name>
       <sld:Title>Geological Unit Lithology Theme</sld:Title>
       <sld:Abstract>The colour has been creatively adapted from Moyer,Hasting
            and Raines, 2005 (http://pubs.usgs.gov/of/2005/1314/of2005-1314.pdf) 
            which provides xls spreadsheets for various color schemes. 
            plus some creative entries to fill missing entries.
       </sld:Abstract>
       <sld:IsDefault>1</sld:IsDefault>
       <sld:FeatureTypeStyle>
         <sld:Rule>
           <sld:Name>acidic igneous material</sld:Name>
           <sld:Abstract>Igneous material with more than 63 percent SiO2.  
                          (after LeMaitre et al. 2002)
           </sld:Abstract>
           <ogc:Filter>
             <ogc:PropertyIsEqualTo>
               <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/
                    gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
               <ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:200811:
                            acidic_igneous_material</ogc:Literal>
             </ogc:PropertyIsEqualTo>
           </ogc:Filter>
           <sld:PolygonSymbolizer>
             <sld:Fill>
               <sld:CssParameter name="fill">#FFCCB3</sld:CssParameter>
             </sld:Fill>
           </sld:PolygonSymbolizer>
         </sld:Rule>
         <sld:Rule>
           <sld:Name>acidic igneous rock</sld:Name>
           <sld:Abstract>Igneous rock with more than 63 percent SiO2.  
                        (after LeMaitre et al. 2002)
           </sld:Abstract>
           <ogc:Filter>
             <ogc:PropertyIsEqualTo>
               <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gsml:composition/
                    gsml:CompositionPart/gsml:lithology/@xlink:href</ogc:PropertyName>
               <ogc:Literal>urn:cgi:classifier:CGI:SimpleLithology:200811:
                            acidic_igneous_rock</ogc:Literal>
               </ogc:PropertyIsEqualTo>
           </ogc:Filter>
           <sld:PolygonSymbolizer>
             <sld:Fill>
               <sld:CssParameter name="fill">#FECDB2</sld:CssParameter>
             </sld:Fill>
           </sld:PolygonSymbolizer>
         </sld:Rule>
         ...
       </sld:FeatureTypeStyle>
     </sld:UserStyle>
    </sld:NamedLayer>
   </sld:StyledLayerDescriptor>
  

GetFeatureInfo
--------------

Read :ref:`wms_getfeatureinfo` for general information on the GetFeatureInfo request. 
Read the tutorial on :ref:`tutorials_getfeatureinfo` for information on how to template the html output.
If you want to store a separate standard template for complex feature collections, save it under the filename
``complex_content.ftl`` in the template directory.

Read the tutorial on :ref:`tutorial_freemarkertemplate` for more information on how to use the freemarker templates.
Freemarker templates support recursive calls, which can be useful for templating complex content.
For example, the following freemarker template creates a table of features with a column for each property, 
and will create another table inside each cell that contains a feature as property: ::

  <#-- 
  Macro's used for content
  -->

  <#macro property node>
      <#if !node.isGeometry>
        <#if node.isComplex>      
        <td> <@feature node=node.rawValue type=node.type /> </td>  
        <#else>
        <td>${node.value?string}</td>
        </#if>
      </#if>
  </#macro>

  <#macro header typenode>
  <caption class="featureInfo">${typenode.name}</caption>
    <tr>
    <th>fid</th>
  <#list typenode.attributes as attribute>
    <#if !attribute.isGeometry>
      <#if attribute.prefix == "">      
          <th >${attribute.name}</th>
      <#else>
          <th >${attribute.prefix}:${attribute.name}</th>
      </#if>
    </#if>
  </#list>
    </tr>
  </#macro>

  <#macro feature node type>
  <table class="featureInfo">
    <@header typenode=type />
    <tr>
    <td>${node.fid}</td>    
    <#list node.attributes as attribute>
        <@property node=attribute />
    </#list>
    </tr>
  </table>
  </#macro>
    
  <#-- 
  Body section of the GetFeatureInfo template, it's provided with one feature collection, and
  will be called multiple times if there are various feature collections
  -->
  <table class="featureInfo">
    <@header typenode=type />

  <#assign odd = false>
  <#list features as feature>
    <#if odd>
      <tr class="odd">
    <#else>
      <tr>
    </#if>
    <#assign odd = !odd>

    <td>${feature.fid}</td>    
    <#list feature.attributes as attribute>
      <@property node=attribute />
    </#list>
    </tr>
  </#list>
  </table>
  <br/>




