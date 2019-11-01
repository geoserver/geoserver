.. _tutorial_freemarkertemplate:

Freemarker Templates
====================

Introduction
------------

This tutorial will introduce you to a more in depth view of what FreeMarker templates are and how you can use the data provided to templates by GeoServer.

`Freemarker <http://www.freemarker.org/>`_ is a simple yet powerful template engine that GeoServer uses whenever developer allowed user customization of outputs. In particular, at the time of writing it's used to allow customization of GetFeatureInfo, GeoRSS and KML outputs.

Freemarker allows for simple variable expansions, as in ``${myVarName}``, expansion of nested properties, such as in ``${feature.myAtt.value}``, up to little programs using loops, ifs and variables.
Most of the relevant information about how to approach template writing is included in the Freemarker's `Designer guide <http://www.freemarker.org/docs/dgui.html>`_ and won't be repeated here: the guide, along with the :ref:`getutorial_kmlplacemark` and :ref:`tutorials_getfeatureinfo` tutorials should be good enough to give you a good grip on how a template is built.

Template Lookup
```````````````

GeoServer looks up templates in three different places, allowing for various level of customization. For example given the ``content.ftl`` template used to generate WMS GetFeatureInfo content:

* Look into ``GEOSERVER_DATA_DIR/workspaces/<workspace>/<datastore>/<featuretype>/content.ftl`` to see if there is a feature type specific template
* Look into ``GEOSERVER_DATA_DIR/workspaces/<workspace>/<datastore>/content.ftl`` to see if there is a store specific template
* Look into ``GEOSERVER_DATA_DIR/workspaces/<workspace>/content.ftl`` to see if there is a workspace specific template
* Look into ``GEOSERVER_DATA_DIR/workspaces/content.ftl`` looking for a global override
* Look into ``GEOSERVER_DATA_DIR/templates/content.ftl`` looking for a global override
* Look into the GeoServer classpath and load the default template

Each templated output format tutorial should provide you with the template names, and state whether the templates can be type specific, or not.  Missing the source for the default template, look up for the service jar in the geoserver distribution (for example, wms-x.y.z.jar), unpack it, and you'll find the actual xxx.ftl files GeoServer is using as the default templates.

Common Data Models
``````````````````

Freemarker calls "data model" the set of data provided to the template. Each output format used by GeoServer will inject a different data model according to the informations it's managing, yet there are three very common elements that appear in almost each template, Feature, FeatureType and FeatureCollection. Here we provide a data model of each.

The data model is a sort of a tree, where each element has a name and a type. Besides basic types, we'll use:

* list: a flat list of items that you can scan thru using the FreeMarker ``<#list>`` directive;
* map: a key/value map, that you usually access using the dot notation, as in ``${myMap.myKey``}, and can be nested;
* listMap: a special construct that is, at the same time, a Map, and a list of the values.

Here are the data models (as you can see there are redundancies, in particular in attributes, we chose this approach to make template building easier):

**FeatureType (map)**

* name (string): the type name
* attributes (listMap): the type attributes
  
  * name (string): attribute  name
  * namespace (string): attribute namespace URI
  * prefix (string): attribute namespace prefix
  * type (string): attribute type,  the fully qualified Java class name
  * isGeometry (boolean): true if the attribute is geometric, false otherwise

**Feature (map)**

* fid (string): the feature ID (WFS feature id)
* typeName (string): the type name
* attributes (listMap): the list of attributes (both data and metadata)
  
  * name (string): attribute  name
  * namespace (string): attribute namespace URI
  * prefix (string): attribute namespace prefix
  * isGeometry (boolean): true if the attribute is geometric, false otherwise  
  * value: a string representation of the the attribute value
  * isComplex (boolean): true if the attribute is a feature (see :ref:`app-schema.complex-features`), false otherwise
  * type (string or FeatureType): attribute type: if isComplex is false, the fully qualified Java class name; if isComplex is true, a FeatureType
  * rawValue: the actual attribute value (is isComplex is true rawValue is a Feature)

* type (map)  

  * name (string): the type name (same as typeName)
  * namespace (string): attribute namespace URI
  * prefix (string): attribute namespace prefix
  * title (string): The title configured in the admin console
  * abstract (string): The abstract for the type
  * description (string): The description for the type
  * keywords (list): The keywords for the type
  * metadataLinks (list): The metadata URLs for the type
  * SRS (string): The layer's SRS
  * nativeCRS (string): The layer's coordinate reference system as WKT

**FeatureCollection (map)**

* features (list of Feature, see above)
* type (FeatureType, see above)


**request (map)**

Contains the GetFeatureInfo request parameters and related values.

**environment (map)**

Allows accessing several environment variables, in particular those defined in:

 * JVM system properties
 * OS environment variables
 * web.xml context parameters 

**Math (map)**

Allows accessing math functions.

Examples
``````````````````
**request**

 * ${request.LAYERS}
 * ${request.ENV.PROPERTY}

**environment**

 * ${environment.GEOSERVER_DATA_DIR}
 * ${environment.WEB_SITE_URL}

**Math**
  
 * ${Math.max(request.NUMBER1,request.NUMBER2)}




