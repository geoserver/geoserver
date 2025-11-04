.. _ogcapi-processes:

OGC API - Processes
===================

A `OGC API - Processes <https://github.com/opengeospatial/ogcapi-processes>`_ based on the current specification draft, delivering the following functionality:

- Process listing
- Process description
- Execution via JSON POST (OGC API 1.0 core)
- Execution via KVP invocation (from OGC API 1.1 DRAFT specification)
- Asynchronous execution and dismissal
- The same wealth of input/output options as in WPS (inline, reference, simple and complex, etc.)

Missing functionality at the time of writing, and known issues:

- API definition is not fully aligned yet
- Conformance class configuration is not available
- Process chaining (not part of Core, see the Workflows DRAFT extension)

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

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download ``ogcapi-processes`` zip archive.
   
   * |version| example: :nightly_community:`ogcapi-processes`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-ogcapi-processes-plugin.zip above).

#. Restart GeoServer.

   On restart the services are listed at http://localhost:8080/geoserver

Configuration of OGC API - Processes module
-------------------------------------------

The module is based on the GeoServer WPS one, follows the same configuration and exposes
the same processes (so for example, it's possible to limit the processes one wants to expose
via the same configuration mechanisms as WPS).


