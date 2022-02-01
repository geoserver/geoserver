.. _wps:

Web Processing Service (WPS)
============================

Web Processing Service (WPS) is an OGC service for the publishing of geospatial processes, algorithms, and calculations.  The WPS service is available as an extension for geoserver providing an execute operation for data processing and geospatial analysis.

WPS is not a part of GeoServer by default, but is available as an extension.

The main advantage of GeoServer WPS over a standalone WPS is **direct integration** with other GeoServer services and the data catalog.  This means that it is possible to create processes based on data served in GeoServer, as opposed to sending the entire data source in the request.  It is also possible for the results of a process to be stored as a new layer in the GeoServer catalog.  In this way, WPS acts as a full remote geospatial analysis tool, capable of reading and writing data from and to GeoServer.

For the official WPS specification, see `OGC Web Processing Service 05-007r7 <http://www.opengeospatial.org/standards/wps>`__.

.. toctree::
   :maxdepth: 2

   install
   operations
   administration
   security
   requestbuilder
   processes/index
   hazelcast-clustering
