.. _params_extractor_install:

Installing the Parameter Extractor extension
============================================

The Parameter Extractor extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

#. Visit the :website:`website download <download>` page, locate your release, and download:  :download_extension:`params-extractor`
   
   Verify that the version number in the filename (for example |release| above) corresponds to the version of GeoServer you are running.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   Make sure you do not create any sub-directories during the extraction process.

#. Restart GeoServer.

If installation was successful, you will see a new Params-Extractor entry in the left menu, under "Settings". 

.. figure:: images/menu.png

   The Parameter Extractor menu entry

