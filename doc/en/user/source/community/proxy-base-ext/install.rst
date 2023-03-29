.. _proxy_base_ext_install:

Installing the Proxy Base extension
=============================================

The Proxy Base extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

#. Visit the :website:`website download <download>` page, locate your release, and download:  :download_extension:`proxy-base-ext`
   
   Verify that the version number in the filename (for example |release| above) corresponds to the version of GeoServer you are running.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   Make sure not to create any sub-directories during the extraction process.

#. Restart GeoServer.

On successful installation, a new Proxy Base Extension entry will appear in the left menu, under "Settings".

.. figure:: images/proxy_base_settings.png

   The Proxy Base Extension menu entry

