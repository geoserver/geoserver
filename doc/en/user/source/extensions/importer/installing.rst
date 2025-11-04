.. _extensions_importer_install:

Installing the Importer extension
=================================

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Other** extensions download **Importer (Core)**.

   * |release| example: :download_extension:`importer`
   * |version| example: :nightly_extension:`importer`
      
   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).
   
   The optional importer download **Importer (BDB Backend)** is used in a clustered environment
   to share state importer progress between nodes.

   * |release| example: :download_extension:`importer-bdb`
   * |version| example: :nightly_extension:`importer-bdb`

#. Extract the archive and copy the contents into the GeoServer :file:`WEB-INF/lib` directory.

#. Restart GeoServer.

#. To verify that the extension was installed successfully, open the :ref:`web_admin` and look for an :guilabel:`Import Data` page in the :guilabel:`Data` section on the left-side navigation menu.

   .. figure:: images/importer_link.png

      Importer extension successfully installed.

For additional information please see the section on :ref:`extensions_importer_using`.
