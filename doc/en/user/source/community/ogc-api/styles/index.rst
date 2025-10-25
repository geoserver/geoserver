OGC API - Styles
================

A `OGC Styles API <https://github.com/opengeospatial/ogcapi-styles>`_ based on the early draft of this specification.

This service describes, retrieves and updates the styles present on the server. 
Styles are cross linked with their associated collections in both the Features and Tiles API.

OGC API - Styles Implementation status
--------------------------------------

.. list-table::
   :widths: 30, 20, 50
   :header-rows: 1

   * - `OGC API - Styles <https://github.com/opengeospatial/ogcapi-styles>`__
     - Version
     - Implementation status
   * - Part 1: Core
     - `Draft <http://docs.opengeospatial.org/DRAFTS/20-009.html>`__
     - Implementation based on early specification draft.
     
Installing the GeoServer OGC API - Styles module
------------------------------------------------

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download ``ogcapi-styles`` zip archive.
   
   * |version| example: :nightly_community:`ogcapi-styles`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-ogcapi-styles-plugin.zip above).

#. Restart GeoServer.

   On restart the services are listed at http://localhost:8080/geoserver

Configuration of OGC API - Styles module
----------------------------------------

At the time of writing the module has no configuration, it's simply exposing all available
GeoServer styles.