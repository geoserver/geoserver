.. _csw_iso_installing:

Installing Catalog Services for Web (CSW) - ISO Metadata Profile
================================================================

To install the CSW ISO extension:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   If you do not have the CSW extension yet, get it first.
   From the list of **OGC Services** extensions download **CSW**.

   * |release| example: :download_extension:`csw`
   * |version| example: :nightly_extension:`csw`
      
   From the list of **OGC Services** extensions download **CSW ISO Metadata Profile**.

   * |release| example: :download_extension:`csw-iso`
   * |version| example: :nightly_extension:`csw-iso`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract these zip files and place all the JARs in ``WEB-INF/lib``.

#. Perform any configuration required by your servlet container, and then restart.

#. Verify that the CSW module was installed correctly by going to the Welcome page of the :ref:`web_admin` and seeing that :guilabel:`CSW` is listed in the :guilabel:`Service Capabilities` list.
   
   Open the CSW capabilities and search for the text `gmd:MD_Metadata` to verify that the ISO metadata profile was installed correctly.
