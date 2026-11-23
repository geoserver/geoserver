# JDBCConfig configuration

The JDBCConfig module is configured in the file `jdbcconfig/jdbcconfig.properties` inside the [GeoServer data directory](../../datadirectory/index.md). The following properties may be set:

- `enabled`: Use JDBCConfig. Turn off to use the data directory for all configuration instead.
- `initdb`: Initialize an empty database if this is set on true.
- `import` : The import configuration option tells GeoServer whether to import the current catalog from the file system to the database or not. If set to true, it will be imported and the config option will be set the value 'false' for the next start up to avoid trying to re-import the catalog configuration.
- `repopulate`: The repopulate configuration option tells GeoServer to repopulate the queryable field values in the database. These are the fields of catalog objects jdbcconfig can query via the database, i.e. much faster than via post-filtering in memory. May be necessary after jdbcconfig upgrades or if someone manually adds queryable fields to the database. (In such cases, if the values were not properly repopulated, queries might give incorrect results.)
- `initScript`: Path to initialisation script .sql file. Only used if initdb = true.

## JNDI

Get the database connection from the application server via JNDI lookup.

- `jndiName`: The JNDI name for the data source. Only set this if you want to use JNDI, the JDBC configuration properties may still be set for in case the JNDI Lookup fails.

## Direct JDBC Connection

Provide the connection parameters directly in the configuration file. This includes the password in the clear which is a potential security risk. To avoid this use JNDI instead.

- `jdbcUrl`: JDBC direct connection parameters.
- `username`: JDBC connection username.
- `password`: JDBC connection password.
- `pool.minIdle`: minimum connections in pool
- `pool.maxActive`: maximum connections in pool
- `pool.poolPreparedStatements`: whether to pool prepared statements
- `pool.maxOpenPreparedStatements`: size of prepared statement cache, only used if `pool.poolPreparedStatements` is true
- `pool.testOnBorrow`: whether to validate connections when obtaining from the pool
- `pool.validationQuery`: validation query for connections from pool, must be set when `pool.testOnBorrow` or `pool.testWhileIdle` is true
- `pool.testWhileIdle`: whether to validate idle connections, used in conjunction with the idle timer below
- `pool.setTimeBetweenEvictionRunsMillis`: period in milliseconds for the idle object evictor, -1 to disable

With `pool.testOnBorrow` set to true, the validation query runs each time GeoServer takes a connection from the pool, which happens many times in a single request, and each run waits for a round trip to the database. Setting `pool.testOnBorrow` to false and `pool.testWhileIdle` to true checks the idle connections in the background instead. This still needs `pool.validationQuery`, and `pool.setTimeBetweenEvictionRunsMillis` set to a few minutes.

## Request cache

JDBCConfig uses two caches for configuration objects.

The first one is shared by all requests. It keeps the objects found in the database, looked up by id or by name, and drops them after a time without use. It does not remember lookups that found nothing, so each such lookup queries the database again.

The second one, the request cache, lives for a single request. It keeps every lookup made during that request, including the ones that found nothing, so asking twice for the same object, or for the same missing object, queries the database only once. When the request ends, GeoServer drops the cache, and the next request starts with an empty one. A change saved by another request on the same GeoServer is therefore visible from the next request on.

To turn the cache off, set the `GEOSERVER_REQUEST_CACHE` system property or environment variable to `false`, for example `-DGEOSERVER_REQUEST_CACHE=false`.
