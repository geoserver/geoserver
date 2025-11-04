.. _stac_data_store_install:

Installing the STAC data store
==============================

The STAC store community module is listed among the other community modules on the GeoServer download page.


The installation process is similar to other GeoServer community modules:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download ``stac-datastore`` zip archive.
   
   * |version| example: :nightly_community:`stac-datastore`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-stac-datastore-plugin.zip above).

#. Restart GeoServer.

   On successful installation there is a new STAC-API datastore entry in the "new Data Source" menu. 

   .. figure:: images/store-selection.png
   
      STAC datastore entry
