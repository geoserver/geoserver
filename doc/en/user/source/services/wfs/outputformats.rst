.. _wfs_output_formats:

WFS output formats
==================

WFS returns features and feature information in a number of possible formats.  This page shows a list of the output formats.  The syntax for setting an output format is::

   outputFormat=<format>

where ``<format>`` is one of the following options:

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
   * - gml32
     - ``outputFormat=gml32``
     - Returns a document in GML 3.2 format
   * - Shapefile
     - ``outputFormat=shape-zip``
     - Returns a shapefile in a ZIP archive
   * - JSON
     - ``outputFormat=application/json``
     - Returns a GeoJSON or a JSON response
   * - JSONP
     - ``outputFormat=text/javascript``
     - Returns a JSONP response in the form: ``parseResponse(...json...)``. Use ``format_options=callback:...`` to change the callback name. This format is disabled by default (See ``ENABLE_JSONP`` in :ref:`wms_global_variables`).
   * - CSV
     - ``outputFormat=csv``
     - Returns a text document in CSV format

.. note:: This list applies to the base GeoServer installation.  Additional output formats (such as Excel XLS) are available using extensions.  The full list of output formats supported by a GeoServer instance can be found by requesting the WFS :ref:`wfs_getcap`.
     
     
Zipped shapefile customisation
------------------------------

Starting with GeoServer version 2.0.3 the zipped shapefile output format output can be customized by creating a Freemarker template to specify the file names of the zip file and the shapefiles in it. The default template looks like the following::

  zip=${typename}
  shp=${typename}${geometryType}
  txt=wfsrequest

Structurally this is a property file.  The ``zip`` property is the name of the zip file, the ``shp`` property the name of the shapefile for a given feature type and ``txt`` is the dump of the WFS request (the request dump is also available starting with version 2.0.3).

The properties available in the template are:
  
* ``typename``: the feature type name (for the zip property it will be the first feature type in case of a request containing many)
* ``geometryType``: the type of geometry contained in the shapefile (it is used only if the output geometry type is generic and the various geometries are fanned out into one shapefile per type)
* ``workspace``: the workspace of the feature type
* ``timestamp``: a Date object with the request timestamp
* ``iso_timestamp``: a string, the ISO timestamp of the request at GMT, in ``yyyyMMdd_HHmmss`` format
  
Format options parameter in WFS requests
----------------------------------------

GeoServer provides the ``format_options`` vendor-specific parameter to specify parameters that are format-specific. The syntax is::

    format_options=param1:value1;param2:value2;...
	
The available format options are:

* ``callback``: specifies the callback function name for the JSONP response format (default is ``parseResponse``).
* ``filename``: Applies only to the SHAPE-ZIP output format. Specifies the name to use as the output filename. For example:  ``format_options=filename:roads.zip``.  If not specified, a filename is derived from the requested feature type name(s).

