.. _smart_data_loader_install:

Installing the Smart Data Loader extension
============================================

The Smart Data Loader extension is listed among the other extension downloads on the GeoServer download page.


The installation process is similar to other GeoServer extensions:

#. Download the Smart Data Loader nightly GeoServer community module from :download_community:`smart-data-loader`.
   
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Make sure you have downloaded and installed the :download_extension:`app-schema` extension.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   Make sure you do not create any sub-directories during the extraction process.

#. Restart GeoServer.

If installation was successful, you will see a new Smart Data Loader entry in the "new Data Source" menu. 

.. figure:: images/store-selection.png

   Smart Data Loader entry
