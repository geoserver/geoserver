.. _ogcapi-tiles:

OGC API - Tiles
===============

A `OGC Tiles API <https://github.com/opengeospatial/OGC-API-Tiles>`_ delivering both tiled data (vector tiles) and tiled maps (classic map tiles).

GeoServer implementation is based on an ealier specification draft (the specification is now finalized). 

OGC API - Tiles Implementation status
-------------------------------------

.. list-table::
   :widths: 30, 20, 50
   :header-rows: 1

   * - `OGC API - Tiles <https://github.com/opengeospatial/ogcapi-tiles>`__
     - Version
     - Implementation status
   * - Part 1: Core
     - `Draft <https://docs.ogc.org/DRAFTS/20-057.html>`__
     - Implementation based on early specification draft, not yet updated to final version


Installing the GeoServer OGC API - Tiles module
------------------------------------------------

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download ``ogcapi-tiles`` zip archive.
   
   * |version| example: :nightly_community:`ogcapi-tiles`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-ogcapi-tiles-plugin.zip above).

#. Restart GeoServer.

   On restart the services are listed at http://localhost:8080/geoserver


Configuration of OGC API - Tiles module
---------------------------------------

The module exposes all Tiled layers configured for GeoWebCache using the OGC API - Tiles specification.

As such, it follows the same WMTS and Tile Caching configuration used to manage GeoWebCache.