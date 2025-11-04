.. _iauwkt.install:

Installing the IAU authority
----------------------------

The IAU authority is an official extension:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Miscellaneous** extensions download **IAU**.

   * |release| example: :download_extension:`iau`
   * |version| example: :nightly_extension:`iau`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the archive and copy the contents into the GeoServer :file:`WEB-INF/lib` directory.

#. Restart GeoServer.

Verify Installation
^^^^^^^^^^^^^^^^^^^

To verify that the extension was installed successfully:

#. On the left menu, get into :guilabel:`Demos` and then :guilabel:`SRS List`

#. Go into the table filter text field, and type `IAU`, then press enter

#. A number of IAU codes should appear in the table

   .. image:: images/srsList.png
      :align: center
      :alt: IAU SRS List
