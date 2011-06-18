.. _wps_processes:

WPS Processes
=============

The Web Processing Service describes a method for publishing geospatial processes, but does not specify what those processes should be.  Servers that implement WPS therefore have complete leeway in what types of processes to implement, as well as how those processes are implemented.  This means that a process request designed for one type of WPS is not expected to work on a different type of WPS.

GeoServer implements processes from two different categories:

* JTS Topology Suite processes
* GeoServer-specific processes

JTS Topology Suite processes
----------------------------

`JTS Topology Suite <http://tsusiatsoftware.net/jts/main.html>`_ is a Java library of functions for processing geometries in two dimensions.  JTS conforms to the Simple Features Specification for SQL published by the Open Geospatial Consortium (OGC), similar to PostGIS.  JTS includes common spatial functions such as area, buffer, intersection, and simplify.

GeoServer WPS implements some of these functions as processes.  The names and definitions of these processes are subject to change, so they have not been included here.  For a full list of JTS processes, please see the GeoServer :ref:`WPS capabilities document <wps_getcaps>`.

GeoServer processes
-------------------

GeoServer WPS includes a few processes created especially for use with GeoServer.  These are usually GeoServer-specific functions, such as bounds and reprojection.  They use an internal connection to the GeoServer WFS/WCS, not part of the WPS specification, for reading and writing data.

As with JTS, the names and definitions of these processes are subject to change, so they have not been included here.  For a full list of GeoServer-specific processes, please see the GeoServer :ref:`WPS capabilities document <wps_getcaps>`.

Process chaining
----------------

One of the benefits of WPS is its native ability to chain processes.  Much like how functions can call other functions, a WPS process can use as its input the output of another process.  Many complex functions can thus be combined in to a single powerful request.

To see WPS requests in action, you can use the built-in :ref:`wps_request_builder`.
