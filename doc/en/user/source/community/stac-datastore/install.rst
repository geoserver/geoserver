.. _stac_data_store_install:

Installing the STAC data store
==============================

The STAC store community module is listed among the other community modules on the GeoServer download page.


The installation process is similar to other GeoServer extensions:

#. Download the STAC store nightly GeoServer community module from :download_community:`stac-datastore`.
   
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   Make sure you do not create any sub-directories during the extraction process.

#. Restart GeoServer.

If installation was successful, you will see a new STAC datastore entry in the "new Data Source" menu. 

.. figure:: images/store-selection.png

   STAC datastore entry
