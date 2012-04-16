.. _wfs_output_formats:

WFS output formats
==================

WFS returns features and feature information in a number of possible formats.  This page shows a list of the output formats.  The syntax for setting an output format is::

   outputFormat=<format>

where ``<format>`` is any of the following options:

.. list-table::
   :widths: 30 30 40
   
   * - **Format**
     - **Syntax**
     - **Notes**
   * - GML2
     - ``outputFormat=GML2``
     - Default option using WFS 1.0.0
   * - GML3
     - ``outputFormat=GML3``
     - Default option using WFS 1.1.0
   * - Shapefile
     - ``outputFormat=shape-zip``
     - Created in a ZIP archive
   * - JSON
     - ``outputFormat=json``
     - 
   * - CSV
     - ``outputFormat=csv``
     - 

.. note:: This list applies to the basic GeoServer installation.  Some additional output formats (such as Excel XLS) are available with the use of an extension.  The full list of output formats supported by a GeoServer instance can be found by requesting the WFS :ref:`wfs_getcap`.
     
     
Zipped shapefile customisation
------------------------------

Starting with GeoServer version 2.0.3 the zipped shapefile output format output can be customized by preparing a Freemarker template which will drive the file names of the zip file and the shapefiles in it. The default template looks like the following::

  zip=${typename}
  shp=${typename}${geometryType}
  txt=wfsrequest

Structurally this is a property file, the ``zip`` property is the name of the zip file, the ``shp`` property the name of the shapefile for a given feature type and ``txt`` is the dump of the WFS request (the request dump is also available starting with version 2.0.3).

The properties available in the template are:
  
  * ``typename``: the feature type name (for the zip property it will be the first feature type in case of a request containing many)
  * ``geometryType``: the type of geometry contained in the shapefile (it used only if the output geometry type is generic and the variuos  geometries are fanned out in one shapefile per type)
  * ``workspace``: the workspace of the feature type
  * ``timestamp``: a Date object with the request timestamp
  * ``iso_timestamp``: a string, the ISO timestamp of the request at GMT, in the yyyyMMdd_HHmmss format
  
Format options as parameter in WFS requests
-------------------------------------------

GeoServer provides the ``format_options`` vendor-specific parameter to specify parameters that are format-specific. The syntax is::

    format-options=param1:value1;param2:value2;...
	
The currently supported format options are:

  * ``filename``: Applies only to the SHAPE-ZIP output format. If a file name is provided, it is used as the output file name. For example:  ``format_options=filename:roads.zip``.  If not specified explicitly, a file name is inferred from the requested feature type(s) name.

