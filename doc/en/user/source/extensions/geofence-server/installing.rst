.. _geofence_server_install:

GeoFence promoted to extension since version 2.15
=================================================

Geofence modules were community modules up to Geoserver version 2.14.
Geofence modules were promoted to extensions since version 2.15
(see `GSIP 164 - Promote geofence modules from Community to Extension <https://github.com/geoserver/geoserver/wiki/GSIP-164>`_.

Up to Geoserver version 2.14, Geofence can be downloaded from `Geoserver build server <https://build.geoserver.org/geoserver/>`_.

Installing the GeoServer GeoFence Server extension
==================================================

 #. Navigate to the `GeoServer download page <http://geoserver.org/download>`_.

 #. Find the page that matches the version of the running GeoServer.

 #. Download the GeoFence extension. The download link will be in the :guilabel:`Extensions` section under :guilabel:`Other`.

 #. Extract the files in this archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.

    .. warning:: By default GeoFence will store his data in a `H2 database <http://www.h2database.com/html/main.html>`_ and the database schema will be automatically managed by Hibernate. `GeoFence documentation <https://github.com/geoserver/geofence/wiki/GeoFence-configuration>`_ explains how to configure a different backed database and configure Hibernate behavior.

 #. Restart GeoServer
