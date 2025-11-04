.. _monitor_installation:

Installing the Monitor Extension
================================

The monitor extension is not part of the GeoServer core and must be installed as a plug-in. To install:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Miscellaneous** extensions download **Monitor (Core)**.

   * |release| example: :download_extension:`monitor`
   * |version| example: :nightly_extension:`monitor`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).
   
#. Extract the files in this archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.

#. Restart GeoServer

Verifying the Installation
---------------------------

There are two ways to verify that the monitoring extension has been properly installed.

#. Start GeoServer and open the :ref:`web_admin`.  Log in using the administration account.  If successfully installed, there will be a :guilabel:`Monitor` section on the left column of the home page.

  .. figure:: images/monitorwebadmin.png
     :align: center

     *Monitoring section in the web admin interface*

#. Start GeoServer and navigate to the current :ref:`datadir`.  If successfully installed, a new directory named ``monitoring`` will be created in the data directory.
