.. _inspire_installing:

Installing the INSPIRE extension
================================

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Other** extensions download **INSPIRE**.

   * |release| example: :download_extension:`inspire`
   * |version| example: :nightly_extension:`inspire`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the archive and copy the contents into the GeoSever :file:`WEB-INF/lib` directory.

#. Restart GeoServer.

To verify that the extension was installed successfully, please see the next section on :ref:`inspire_using`.
