.. _ogcapi-processes:

OGC API - Processes
===================

A `OGC API - Maps <https://github.com/opengeospatial/ogcapi-processes>`_ based on the current specification draft, delivering partial functionality:

- Process listing
- Process description
- Synchronous execution via KVP invocation

Missing functionality at the time of writing, and known issues:

- API definition is not fully aligned yet
- Exection via POST
- Remote URL inputs
- Asynchronous exectuion

OGC API - Processes Implementation status
-----------------------------------------

.. list-table::
   :widths: 30, 20, 50
   :header-rows: 1

   * - `OGC API - Maps <https://github.com/opengeospatial/ogcapi-processes>`__
     - Version
     - Implementation status
   * - Part 1: Core
     - `Draft <https://docs.ogc.org/is/18-062r2/18-062r2.html>`__
     - Implementation based on current specification draft (KVP is not part of Processes 1.0, but it's part of the current draft)

Installing the GeoServer OGC API - Processes module
---------------------------------------------------

#. Download the OGC API nightly GeoServer community module from :download_community:`ogcapi-processes`.
   
   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|release|-ogcapi-processes-plugin.zip above).

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. On restart the services are listed at http://localhost:8080/geoserver

Configuration of OGC API - Processes module
-------------------------------------------

The module is based on the GeoServer WPS one, follows the same configuration and exposes
the same processes (so for example, it's possible to limit the processes one wants to expose
via the same configuration mechanisms as WPS).


