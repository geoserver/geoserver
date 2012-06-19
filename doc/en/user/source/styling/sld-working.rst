.. _sld_working:

Working with SLD
================

This section describes how to create, view and troubleshoot SLD styling documents in GeoServer.

Authoring
---------

GeoServer comes bundled with a few basic styles, and any number of new styles can be added.  
Styles can be viewed, edited and validated via the :ref:`webadmin_styles` menu of the :ref:`web_admin`. 

Every layer (featuretype) registered with GeoServer must have at least one style associated with it.  
Any number of additional styles can be associated with a layer,
one of which is defined to be the default style for rendering the layer.
This allows layers to have appropriate styles advertised in the WMS ``GetCapabilities`` document.
A layer's styles can be changed at any time 
using the :ref:`webadmin_layers` page of the :ref:`web_admin`.  
Note that when adding a layer and a style to GeoServer at the same time, the style should be added first, 
so that the new layer can be associated with the style immediately. 

Viewing
-------

Once a style has been associated with a layer, the resulting rendering of the layer data
can be viewed by using the :ref:`layerpreview`. 

To view the effect of compositing multiple styled layers, two approaches are possible:

* Create a **layer group** for the desired layers using the :ref:`webadmin_layergroups` page, and preview it.  
  Non-default styles can be specified for layers if required.
* Submit a WMS ``GetMap`` request specifying the layers, and the desired styles if they are not the defaults


Troubleshooting
---------------

SLD is a type of programming language, not unlike creating a web page or building a script.  
As such, problems may arise that may require troubleshooting.  
When adding a style into GeoServer, it is automatically validated against the OGC SLD specification (although that may be bypassed), but it will not be checked for semantic errors.  
It is easy to have errors creep into a valid SLD.  
Most of the time this will result in a map displaying no features (a blank map), 
but sometimes errors will prevent the map from even loading at all.

The easiest way to fix errors in an SLD is to try to isolate the error.  
If the SLD is long and incorporates many different rules and filters, try temporarily removing some of them to see if the errors go away.

To minimize syntax errors when creating the SLD, 
it is recommended to use a text editor that is designed to work with XML
(such as the :guilabel:`Style Editor` provided int the GeoServer UI).  
XML editors can make finding errors much easier by providing syntax highlighting and (sometimes) built-in error checking.