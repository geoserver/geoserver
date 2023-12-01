.. _extensions_importer_install:

Installing the Importer extension
=================================

#. Visit the :website:`website download <download>` page, locate your release, and download:  :download_extension:`importer`
   
   Verify that the version number in the filename (for example |release| above) corresponds to the version of GeoServer you are running.

#. Extract the archive and copy the contents into the GeoServer :file:`WEB-INF/lib` directory.

#. Restart GeoServer.

#. To verify that the extension was installed successfully, open the :ref:`web_admin` and look for an :guilabel:`Import Data` option in the :guilabel:`Data` section on the left-side menu.

   .. figure:: images/importer_link.png

      Importer extension successfully installed.

For additional information please see the section on :ref:`extensions_importer_using`.
