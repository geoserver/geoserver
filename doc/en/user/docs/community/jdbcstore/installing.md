---
render_macros: true
---


# Installing JDBCStore

To install the JDBCStore module:

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download ``jdbcstore`` zip archive.

    - {{ snapshot }} example: [jdbcstore](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-jdbcstore-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

    The JDBCStore plug-in includes the [JDBCConfig](../jdbcconfig/index.md) plugin as well which will generally be run at the same time.

4.  Perform any configuration required by your servlet container, and then restart.

    On startup, JDBCStore will create a configuration directory **`jdbcstore`** and JDBCConfig will create a configuration directory **`jdbcconfig`** in the [GeoServer data directory](../../datadirectory/index.md) .

5.  Verify that the configuration directories were created to be sure installation worked then turn off GeoServer.

6.  If you want to use [JDBCConfig](../jdbcconfig/index.md) as well, configure it first, being sure to set `enabled`, `initdb`, and `import` to `true`, and to provide the connection information for an empty database. Start GeoServer to initialize the JDBCConfig database, import the old catalog into it, and take over from the old catalog. Subsequent start ups will skip the initialize and import steps unless you re-enable them in `jdbcconfig.properties`.

7.  Now configure JDBCStore in a similar fashion ([JDBCStore configuration](configuration.md)), being sure to set `enabled`, `initdb`, and `import` to `true`, and to provide the connection information for an empty database. Start GeoServer again. This time JDBCStore will connect to the specified database, initialize it, import the old [GeoServer data directory](../../datadirectory/index.md) into it, and take over from the old [GeoServer data directory](../../datadirectory/index.md). Subsequent start ups will skip the initialize and import steps unless you re-enable them in `jdbcstore.properties`.
