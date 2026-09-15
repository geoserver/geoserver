# Migrating GeoFence configuration

## GeoServer 3.0

### H2 compatibility no longer available

The embedded GeoFence engine no longer bundles an H2 driver or dialect; `postgresql`/`postgis-jdbc` are the
only supported database drivers. If your `geofence-datasource.properties` (or the previous
`geofence-datasource-ovr.properties`) pointed at an H2 database, you must migrate your data to
PostgreSQL/PostGIS before upgrading - this was already discouraged as of GeoServer 2.27, see below.

### Datasource configuration file renamed, no more bundled default

The embedded GeoFence engine's database configuration file has been renamed from
`geofence-datasource-ovr.properties` to `geofence-datasource.properties`, and is no longer looked up on the
classpath. GeoServer now looks for it, in order:

1. An absolute path given by the `GEOFENCE_DATASOURCE_FILE` system property or environment variable.
2. A relative filename given by the same property/variable, resolved against `<data directory>/geofence/`.
3. The plain filename `geofence-datasource.properties`, resolved against `<data directory>/geofence/` or the
   current working directory.

There is no built-in default database anymore. If you previously relied on the bundled default connection
(no config file present at all), you must now create a `geofence-datasource.properties` file yourself before
starting GeoServer, or startup will fail fast with an error listing every path that was checked.

### GeoFence authentication provider removed

Installing `geofence`/`geofence-server` on 2.x registered GeoFence as an available authentication provider,
leaving a config entry for it under `<DATADIR>/security/auth/` (by default
`security/auth/geofence/config.xml`). The GeoFence authentication provider has been intentionally removed
in this porting, and this leftover config is not compatible with it.

Before upgrading, delete that config entry from the data directory - the whole `geofence` provider
directory, not just `config.xml` - otherwise it is silently left behind, unused and incompatible, rather
than causing a visible error.


### `geofence-default-override.properties` (`PropertyOverrideConfigurer`) replaced for datasource tuning

The old `geofence-default-override.properties` mechanism allowed overriding arbitrary Spring bean
properties (a Spring `PropertyOverrideConfigurer`, e.g. `geofenceDataSource.url=...`). For datasource/JPA
tuning, this has been replaced by two explicit, narrower property prefixes read directly from
`geofence-datasource.properties`:

- `geofence.hibernate.*` — passed through to Hibernate/JPA, with the prefix stripped (e.g.
  `geofence.hibernate.hbm2ddl.auto=validate`).
- `geofence.datasource.hikari.*` — passed through to HikariCP's connection pool configuration, with the
  prefix stripped.

If you were using `geofence-default-override.properties` to tune Hibernate or connection-pool settings,
move those settings into `geofence-datasource.properties` under the prefixes above. This does **not** cover
overriding LDAP-related beans (DAO selection, connection settings, attribute mappers) - that older use of
`PropertyOverrideConfigurer` has not been replaced yet.

### REST API: `*Any` query parameters are deprecated

On `GET /rules`, `GET /rules/count` and the equivalent `/adminrules` endpoints, every `*Any` filter
parameter (e.g. `userAny`, `roleAny`, `workspaceAny`) is deprecated in favor of an equivalently-named
`*Default` parameter (e.g. `userDefault`, `roleDefault`, `workspaceDefault`) - same meaning, renamed for
clarity. See [Filter Parameters](rest.md#filter-parameters) for the full list. The old `*Any` names still
work as a fallback when the matching `*Default` isn't set; new clients should use `*Default`.

Note: some GeoFence REST clients send `groupAny`/`groupDefault` for the role filter instead of
`roleAny`/`roleDefault` - older terminology from when GeoFence managed its own users and groups, before
most deployments moved to GeoServer's roles. This embedded engine's REST API only accepts `role*`.

### REST API: `instanceId` query parameter is deprecated

`instanceId`, which filters on a GeoServer instance's internal numeric id, is deprecated in favor of
`instanceName`. A request that still sends `instanceId` logs a warning. `instanceId` and `instanceName`
remain mutually exclusive - specifying both is still rejected, currently with a `500` error rather than a
`400` (a pre-existing issue, not addressed here).

### REST API: `validAfterString`/`validBeforeString` are now ISO-8601

The bulk-configuration REST endpoints (`RESTRule`, and by extension `RESTFullRuleList`/
`RESTFullConfiguration`) previously rendered the `validAfterString`/`validBeforeString` JSON fields using
Java's free-form `Date.toString()` format (e.g. `"Thu Jan 01 01:00:00 CET 2026"`), which also varied with
the server's local timezone. These now render as ISO-8601 UTC instants (e.g. `"2026-01-01T00:00:00Z"`,
matching the `validAfter`/`validBefore` XML representation). Any client parsing the old free-form string
needs to be updated; the `validAfter`/`validBefore` fields themselves (epoch millis in JSON, `dateTime` in
XML) are unchanged.

### Specific Java options:

In previous versions there was a conflict with GWC, so the `-Dgwc.context.suffix=gwc` was recommended.
This is no longer needed.


## GeoServer 2.27

In GeoServer 2.27, GeoFence dependency for H2 moved from major version 1 to 2.3.

The file format in the new version is not fully compatible with the previous one, so you may want to follow the instructions on the [H2 migration page](https://www.h2database.com/html/migration-to-v2.md) to use the new H2 version.

If you are using H2 as a backend for GeoFence, please note that this is strongly discouraged and you should move to postgres/postgis or other spatially enabled DBMS.

## GeoServer 2.12

Starting from GeoServer 2.12, the `allowDynamicStyles` GeoFence configuration option has been moved to the core GeoServer WMS module.

This means that if you had this option active in GeoFence, you have to manually enable the same option in the WMS service configuration page of the GeoServer Admin UI (either globally or on a virtual service by virtual service basis).

See here: [WMS settings](../../services/wms/webadmin.md)
