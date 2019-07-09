.. _wfs_output_formats:


WFS output formats
==================

WFS returns features and feature information in a number of formats. The syntax for specifying an output format is::

   outputFormat=<format>

where ``<format>`` is one of the following options:

.. list-table::
   :widths: 15 30 55
   :header-rows: 1
   
   * - Format
     - Syntax
     - Notes
   * - GML2
     - ``outputFormat=GML2``
     - Default option for WFS 1.0.0
   * - GML3
     - ``outputFormat=GML3``
     - Default option for WFS 1.1.0 and 2.0.0
   * - Shapefile
     - ``outputFormat=shape-zip``
     - ZIP archive will be generated containing the shapefile (see :ref:`wfs_outputformat_shapezip` below).
   * - JSON
     - ``outputFormat=application/json``
     - Returns a GeoJSON or a JSON output. Note ``outputFormat=json`` is only supported for getFeature (for backward compatibility).
   * - JSONP
     - ``outputFormat=text/javascript``
     - Returns a `JSONP <http://en.wikipedia.org/wiki/JSONP>`_ in the form: ``parseResponse(...json...)``. See :ref:`wms_vendor_parameters` to change the callback name. Note that this format is disabled by default (See :ref:`wms_global_variables`).
   * - CSV
     - ``outputFormat=csv``
     - Returns a CSV (comma-separated values) file

.. note:: Some additional output formats (such as :ref:`Excel <excel_extension>`) are available with the use of an extension. The full list of output formats supported by a particular GeoServer instance can be found by performing a WFS :ref:`wfs_getcap` request.

GeoServer provides the ``format_options`` vendor-specific parameter to specify parameters that are specific to each format. The syntax is:

::

    format-options=param1:value1;param2:value2;...

.. _wfs_outputformat_shapezip:

Shapefile output
----------------

The shapefile format has a number of limitations that would prevent turning data sources into an equivalent shapefile. In order to abide with such limitations
the shape-zip output format will automatically apply some transformations on the source data, and eventually split the single colleciton into multiple
shapefiles. In particular, the shape-zip format will:

* Reduce attribute names to the DBF accepted length, making sure there are not conflicts (counters being added at the end of the attribute name to handle this).
* Fan out multiple geometry type into parallel shapefiles, named after the original feature type, plus the geometry type as a suffix.
* Fan out multiple shapefiles in case the maximum size is reached

The default max size for both .shp and .dbf file is 2GB, it's possible to modify those limits by setting the GS_SHP_MAX_SIZE and 
GS_DBF_MAX_SIZE system variables to a different value (as a byte count, the default value being 2147483647).

Shapefile output ``format_options``:

* ``format_option=filename:<zipfile>``: if a file name is provided, the name is used as the output file name. For example, ``format_options=filename:roads.zip``.

Shapefile filename customization
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If a file name is not specified, the output file name is inferred from the requested feature type name. The shapefile output format output can be customized by preparing a :ref:`Freemarker template <tutorial_freemarkertemplate>` which will configure the file name of the archive (ZIP file) and the files it contains. The default template is:

::

  zip=${typename}
  shp=${typename}${geometryType}
  txt=wfsrequest

The ``zip`` property is the name of the archive, the ``shp`` property is the name of the shapefile for a given feature type, and ``txt`` is the dump of the actual WFS request.

The properties available in the template are:
  
  * ``typename``—Feature type name (for the ``zip`` property this will be the first feature type if the request contains many feature types)
  * ``geometryType``—Type of geometry contained in the shapefile. This is only used if the output geometry type is generic and the various geometries are stored in one shapefile per type.
  * ``workspace``—Workspace of the feature type
  * ``timestamp``—Date object with the request timestamp
  * ``iso_timestamp``—String (ISO timestamp of the request at GMT) in ``yyyyMMdd_HHmmss`` format
  
JSON and JSONP output
---------------------

The JSON output format (and JSONP if enabled) return feature content as a `GeoJSON <http://geojson.org/>`__ document.  Here is an example of a simple GeoJSON file;

.. code-block:: json

   {  "type": "Feature",
      "geometry": {
         "type": "Point",
         "coordinates": [125.6, 10.1]
      },
      "properties": {
         "name": "Dinagat Islands"
      }
   }

The output properties can include the use of lists and maps:

.. code-block:: json

    {
      "type": "Feature",
      "id": "example.3",
      "geometry": {
        "type": "POINT",
        "coordinates": [ -75.70742, 38.557476 ],
      },
      "geometry_name": "geom",
      "properties": {
        "CONDITION": "Orange",
        "RANGE": {"min":"37","max":"93"}
      }
    }

JSON output ``format_options``:

* ``format_options=id_policy:<attribute name>=<attribute|true|false>`` is used to determine if the id values are included in the output.
   
   Use ``format_options=id_policy:reference_no`` for feature id generation using the reference_no attribute, or ``format_options=id_policy:reference_no=true`` for default feature id generation, or ``format_options=id_policy:reference_no=false`` to suppress feature id output.
   
   If id_policy is not specified the geotools default feature id generation is used.

* ``format_options=callback:<parseResponse>`` applies only to the JSONP output format. See :ref:`wms_vendor_parameters` to change the callback name. Note that this format is disabled by default (See :ref:`wms_global_variables`).

JSON output ``system properties``:

* ``json.maxDepth=<max_value>`` is used to determine the max number of allowed JSON nested objects on encoding phase.  By default the value is 100.
