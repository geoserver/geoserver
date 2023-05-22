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

#. Download the OGC API nightly GeoServer community module from :download_community:`ogcapi-tiles`.
   
   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|release|-ogcapi-tiles-plugin.zip above).

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. On restart the services are listed at http://localhost:8080/geoserver


Configuration of OGC API - Tiles module
---------------------------------------

The module exposes all Tiled layers configured for GeoWebCache using the OGC API - Tiles specification.

As such, it follows the same WMTS and Tile Caching configuration used to manage GeoWebCache.