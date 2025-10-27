.. _geofence_install:

Installing the GeoServer GeoFence extension
===========================================

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Security** extensions download **GeoFence Client**.

   * |release| example: :download_extension:`geofence`
   * |version| example: :nightly_extension:`geofence`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the files in this archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.

#. Restart GeoServer