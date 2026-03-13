# DuckDB DataStore Extension for GeoServer

This community module packages the GeoTools DuckDB datastore (`gt-duckdb`) for GeoServer.

## Scope

- Enables DuckDB datastore usage in GeoServer through standard datastore configuration.
- Uses GeoServer's generic datastore edit UI (no custom store panel in this module).
- Provides standard REST datastore coverage for create/update/delete flows.

## Connection parameters

The main datastore parameters are:

- `dbtype=duckdb`
- `memory=true|false` (default `false`)
- `database=/path/to/file.duckdb` (required when `memory=false`, forbidden when `memory=true`)
- `read_only=true|false` (default `true`)

By default the store runs in hardened read-only mode. Set `read_only=false` only when managed write operations are required.
