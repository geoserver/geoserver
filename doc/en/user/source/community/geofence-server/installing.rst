.. _geofence_server_install:

Installing the GeoServer GeoFence Server extension
==================================================

 #. Download the extension from the `nightly GeoServer community module builds <http://ares.opengeo.org/geoserver/master/community-latest/>`_.

    .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

 #. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

    .. warning:: By default GeoFence will store his data in a `H2 database <http://www.h2database.com/html/main.html>`_ and the database schema will be automatically managed by Hibernate. `GeoFence documentation <https://github.com/geoserver/geofence/wiki/GeoFence-configuration>`_ explains how to configure a different backed database and configure Hibernate behavior.