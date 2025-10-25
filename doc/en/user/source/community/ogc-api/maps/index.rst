.. _ogcapi-maps:

OGC API - Maps
==============

A `OGC API - Maps <https://github.com/opengeospatial/ogcapi-maps>`_ based on the current early specification draft, delivering partial functionality:

- Collection listing
- Styles per collection
- Map and info support for single collection

Missing functionality at the time of writing, and known issues:

- API definition
- Maps specific metadata (e.g., scale ranges)
- Support for multi-collection map representation
- HTML representation of a map with OpenLayers is only partially functional

OGC API - Maps Implementation status
------------------------------------

.. list-table::
   :widths: 30, 20, 50
   :header-rows: 1

   * - `OGC API - Maps <https://github.com/opengeospatial/ogcapi-maps>`__
     - Version
     - Implementation status
   * - Part 1: Core
     - `Draft <https://docs.ogc.org/DRAFTS/20-057.html>`__
     - Implementation based on early specification draft.
   * - Part 2: Partitioning
     - `Draft <https://github.com/opengeospatial/ogcapi-maps/tree/master/extensions/partitioning/standard>`__
     - Implementation based on early specification draft.

Installing the GeoServer OGC API - Maps module
------------------------------------------------

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download ``ogcapi-maps`` zip archive.
   
   * |version| example: :nightly_community:`ogcapi-maps`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-ogcapi-maps-plugin.zip).


#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

#. Restart GeoServer.

   On restart the services are listed at http://localhost:8080/geoserver

Configuration of OGC API - Maps module
--------------------------------------

The module is based on the GeoServer WMS one, follows the same configuration and exposes
the same layers. As a significant difference, Maps does not have a concept of layer tree,
so only individual layers and groups can be exposed.


