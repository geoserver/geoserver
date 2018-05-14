.. _monitor_hibernate_installation:

Installing the Hibernate Monitor Extension
==========================================

.. note::
  
     If performing an upgrade of the monitor extension please see :ref:`monitor_upgrade`. 
  
As a community module, the package needs to be downloaded from the `nightly builds <https://build.geoserver.org/geoserver/>`_,
picking the community folder of the corresponding GeoServer series (e.g. if working on GeoServer master nightly
builds, pick the zip file form ``master/community-latest``).

To install the module, unpack the zip file contents into GeoServer own ``WEB-INF/lib`` directory and
restart GeoServer.

For the module to work, the :ref:`monitor_extension` extensions must also be installed.