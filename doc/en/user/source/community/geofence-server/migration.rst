.. _geofence_server_migration:

Migrating old GeoFence configuration to GeoServer 2.12 and following
====================================================================

Starting from GeoServer 2.12, the ``allowDynamicStyles`` GeoFence configuration
option has been moved to the core GeoServer WMS module.

This means that if you had this option active in GeoFence, you have to manually
enable the same option in the WMS service configuration page of the GeoServer
Admin UI (either globally or on a virtual service by virtual service basis).

See here: :ref:`services_webadmin_wms`