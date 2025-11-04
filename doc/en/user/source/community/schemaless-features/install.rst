Installing the Schemaless Mongo module
=========================================

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download ``mongodb-schemaless`` zip archive.
   
   * |version| example: :nightly_community:`mongodb-schemaless`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-mongodb-schemaless-plugin.zip above).

#. Restart GeoServer.

   On restart the ``MongoDB Schemaless`` vector source option is available from the ``New Data Source`` page:

   .. figure:: images/new-data-sources-schemaless.png

