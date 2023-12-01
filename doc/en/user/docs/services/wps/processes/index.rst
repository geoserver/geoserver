.. _wps_processes:

Process Cookbook
================

The Web Processing Service describes a method for publishing geospatial processes, but does not specify what those processes should be.  Servers that implement WPS therefore have complete leeway in what types of processes to implement, as well as how those processes are implemented.  This means that a process request designed for one type of WPS is not expected to work on a different type of WPS.

GeoServer gathers processes into several different categories based on subject. These categories are grouped by prefix:

* geo: geometry processes
* ras: raster processes
* vec: Vector processes
* gs: GeoServer-specific processes

This cookbook provides examples of some of the available process. Unless otherwise stated examples were generated with the :ref:`wps_request_builder` using the sample data included with each GeoServer release.

.. toctree::
   :maxdepth: 2

   geo
   gs
   chaining

.. note:: 
   
   Previous releases of GeoServer grouped processes not by subject, but by the internal library responsible for implementation. The "JTS" and "gt" prefixes can be enabled to preserve backwards compatibility, or you may safely disable them off - their functionality is correctly sorted into the "vec" and "geo" categories.