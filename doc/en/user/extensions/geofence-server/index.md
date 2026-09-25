# Geofence Embedded Server

This plugin runs a [GeoFence](https://github.com/geoserver/geofence/) server integrated internally in GeoServer. Geofence allows far more advanced security configurations than the default GeoServer [Security](../../security/index.md) subsystem, such as rules that combine data and service restrictions.

In the integrated version, the users and roles service configured in geoserver are associated with the geofence rule database. The integrated geofence server can be configured using its WebGUI page or REST configuration.

<div class="grid cards" markdown>

- [Installing the plugin](installing.md)
- [GeoFence Server GUI](gui.md)
- [GeoFence REST API](rest.md)
- [REST API: AdminRules](rest-adminrule.md)
- [REST API: Batch](rest-batch-op.md)
- [Using the Embedded GeoFence server (Tutorial)](tutorial.md)
- [Migrating configuration](migration.md)

</div>
