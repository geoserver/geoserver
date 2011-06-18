.. _monitor_installation:

Installing the Monitoring Extension
===================================

Monitoring is a community extension, and thus is not found on the standard GeoServer release download pages.  Community extensions are only available via `Nightly builds <http://geoserver.org/display/GEOS/Nightly>`_ or by compiling from source.

#. Download the proper "monitoring" extension linked from the `GeoServer nightly builds page <http://geoserver.org/display/GEOS/Nightly>`_.

   .. warning:: Ensure the extension matching the version of the GeoServer installation is downloaded.

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of
   the GeoServer installation.

Verifying the Installation
---------------------------

There are two ways to verify that the monitoring extension has been properly installed.

* Start GeoServer and open the :ref:`web_admin`.  Log in using the administration account.  If successfully installed, there will be a :guilabel:`Monitor` section on the left column of the home page.

  .. figure:: images/monitorwebadmin.png
     :align: center

     *Monitoring section in the web admin interface*

* Start GeoServer and navigate to the current :ref:`data_directory`.  If successfully installed, a new directory named ``monitoring`` will be created in the data directory.
