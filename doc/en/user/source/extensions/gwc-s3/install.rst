.. _gwc_s3_install:

Installing the GWC S3 extension
===============================

The installation process is similar to other GeoServer extensions:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Other** extensions download **GWC S3 tile storage**.

   * |release| example: :download_extension:`gwc-s3`
   * |version| example: :nightly_extension:`gwc-s3`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.
   Make sure you do not create any sub-directories during the extraction process.

#. Restart GeoServer.

#. To verify the installation was successful, to "Tile Caching", "Blobstores" and create
   a new blobstore, the S3 option show be available: 
   
   .. figure:: img/newBlobstore.png
   
      The S3 option showing while creating a new blobstore  
   
