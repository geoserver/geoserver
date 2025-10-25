.. _ogcapi-coverages:

OGC API - Coverages
===================

A `OGC API - Coverages <https://github.com/opengeospatial/ogcapi-coverages>`_ based on an earlier specification draft, delivering partial functionality:

- Collection listing
- Download in the same formats supported by WCS
- Spatial and temporal subsetting
- Mosaic index filtering (GeoServer extension)
- Domain set description in JSON

Missing functionality at the time of writing, and known issues:

- Full coverage JSON  support
- Scaling
- CRS transformation

OGC API - Coverages Implementation status
-----------------------------------------

.. list-table::
   :widths: 30, 20, 50
   :header-rows: 1

   * - `OGC API - Coverages <https://github.com/opengeospatial/ogcapi-coverages>`__
     - Version
     - Implementation status
   * - Part 1: Core
     - `Draft <https://docs.ogc.org/DRAFTS/19-087.html>`__
     - Implementation based on early specification draft, not yet updated to final version

Installing the GeoServer OGC API - Coverages module
---------------------------------------------------

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download `ogcapi-coverages` zip archive.
   
   * |version| example: :nightly_community:`ogcapi-coverages`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-ogcapi-coverages-plugin.zip above).

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

#. Restart GeoServer.

#. On restart the services are listed at http://localhost:8080/geoserver

Configuration of OGC API - Covearges module
-------------------------------------------

The module is based on the GeoServer WCS one, follows the same configuration and exposes
the same coverages.
