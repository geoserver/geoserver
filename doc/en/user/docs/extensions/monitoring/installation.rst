.. _monitor_installation:

Installing the Monitor Extension
================================

.. note::
  
     If performing an upgrade of the monitor extension please see :ref:`monitor_upgrade`. 
  
The monitor extension is not part of the GeoServer core and must be installed as a plug-in. To install:

#. Visit the :website:`website download <download>` page, locate your release, and download:  :download_extension:`monitor`

   The download link will be in the :guilabel:`Extensions` section under :guilabel:`Other`.
   
   .. warning:: Make sure to match the version of the extension (for example |release| above) to the version of the GeoServer instance!
   
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
