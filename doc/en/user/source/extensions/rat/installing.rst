.. _rat_installing:

Installing the RAT module
=========================

To install the Raster Attribute Table support:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Coverage Formats** extensions download **Raster Attribute Table**.

   * |release| example: :download_extension:`rat`
   * |version| example: :nightly_extension:`rat`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).


#. Extract these files and place the JARs in ``WEB-INF/lib``.

#. Perform any configuration required by your servlet container, and then restart.
