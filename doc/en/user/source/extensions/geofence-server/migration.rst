.. _geofence_server_migration:


Migrating GeoFence configuration
================================

GeoServer 2.27
--------------

In GeoServer 2.27, GeoFence dependency for H2 moved from major version 1 to 2.3.

The file format in the new version is not fully compatible with the previous one, so you may want
to follow the instructions on the `H2 migration page <https://www.h2database.com/html/migration-to-v2.html>`__
to use the new H2 version.

If you are using H2 as a backend for GeoFence, please note that this is strongly discouraged and you should
move to postgres/postgis or other spatially enabled DBMS.


GeoServer 2.12
--------------

Starting from GeoServer 2.12, the ``allowDynamicStyles`` GeoFence configuration
option has been moved to the core GeoServer WMS module.

This means that if you had this option active in GeoFence, you have to manually
enable the same option in the WMS service configuration page of the GeoServer
Admin UI (either globally or on a virtual service by virtual service basis).

See here: :ref:`services_webadmin_wms`
