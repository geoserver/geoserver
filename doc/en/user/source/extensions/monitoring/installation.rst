.. _monitor_installation:

Installing the Monitor Extension
================================

.. note::
  
     If performing an upgrade of the monitor extension please see :ref:`monitor_upgrade`. 
  
The monitor extension is not part of the GeoServer core and must be installed as a plug-in. To install:

#. Navigate to the `GeoServer download page <http://geoserver.org/download>`_.
#. Find the page that matches the version of the running GeoServer.
#. Download the monitor extension. The download link will be in the :guilabel:`Extensions` 
   section under :guilabel:`Other`.
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
