# Geofence Internal Server

This plugin runs a [GeoFence](https://github.com/geoserver/geofence/) server integrated internally in GeoServer. Geofence allows far more advanced security configurations than the default GeoServer [Security](../../security/index.md) subsystem, such as rules that combine data and service restrictions.

In the integrated version, the users and roles service configured in geoserver are associated with the geofence rule database. The integrated geofence server can be configured using its WebGUI page or REST configuration.

<div class="grid cards" markdown>

-   [Installing the GeoServer GeoFence Server extension](installing.md)
-   [GeoFence Server GUI](gui.md)
-   [GeoFence Rest API](rest.md)
-   [AdminRules Rest API](rest-adminrule.md)
-   [Batch Rest API](rest-batch-op.md)
-   [Using the Internal GeoFence server (Tutorial)](tutorial.md)
-   [Migrating old GeoFence configuration to GeoServer 2.12 and following](migration.md)

</div>
