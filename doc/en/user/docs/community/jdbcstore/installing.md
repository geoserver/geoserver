# Installing JDBCStore {: #community_jdbcstore_installing }

To install the JDBCStore module:

1.  Download the module: `jdbcstore`{.interpreted-text role="download_community"}

    The JDBCStore plug-in automatically includes the [JDBCConfig](../jdbcconfig/index.md) plugin as well which will generally be run at the same time.

2.  Extract this file and place the JARs in `WEB-INF/lib`.

3.  Perform any configuration required by your servlet container, and then restart. On startup, JDBCStore will create a configuration directory `jdbcstore` and JDBCConfig will create a configuration directory `jdbcconfig` in the [GeoServer data directory](../../datadirectory/index.md) .

4.  Verify that the configuration directories were created to be sure installation worked then turn off GeoServer.

5.  If you want to use [JDBCConfig](../jdbcconfig/index.md) as well, configure it first, being sure to set `enabled`, `initdb`, and `import` to `true`, and to provide the connection information for an empty database. Start GeoServer to initialize the JDBCConfig database, import the old catalog into it, and take over from the old catalog. Subsequent start ups will skip the initialize and import steps unless you re-enable them in `jdbcconfig.properties`.

6.  Now configure JDBCStore in a similar fashion ([JDBCStore configuration](configuration.md)), being sure to set `enabled`, `initdb`, and `import` to `true`, and to provide the connection information for an empty database. Start GeoServer again. This time JDBCStore will connect to the specified database, initialize it, import the old [GeoServer data directory](../../datadirectory/index.md) into it, and take over from the old [GeoServer data directory](../../datadirectory/index.md). Subsequent start ups will skip the initialize and import steps unless you re-enable them in `jdbcstore.properties`.
