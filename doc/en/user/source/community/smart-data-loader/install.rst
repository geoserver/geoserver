.. _smart_data_loader_install:

Installing the Smart Data Loader extension
============================================

The Smart Data Loader community module is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer community modules:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Make sure you have downloaded and installed the `app-schema` extension first.

   Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Vector Formats** extensions download **App Schema**.

   * |release| example: :download_extension:`app-schema`
   * |version| example: :nightly_extension:`app-schema`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Then you can download and install the Smart Data Loader.

   Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   * |version| example: :nightly_community:`smart-data-loader`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>` for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   Make sure you do not create any sub-directories during the extraction process.

#. Restart GeoServer.

   When installation is successful, a Smart Data Loader entry is available in "new Data Source" menu. 

   .. figure:: images/store-selection.png
   
      Smart Data Loader entry
