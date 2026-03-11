---
render_macros: true
---


# Installing JDBCConfig

To install the JDBCConfig module:

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download ``jdbcconfig`` zip archive.

    - {{ snapshot }} example: [jdbcconfig](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-jdbcconfig-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

3.  Extract this file and place the JARs in `WEB-INF/lib`.

4.  Perform any configuration required by your servlet container, and then restart. On startup, JDBCConfig will create a configuration directory `jdbcconfig` in the [GeoServer data directory](../../datadirectory/index.md).

5.  Verify that the configuration directory was created to be sure installation worked then turn off GeoServer.

6.  Configure JDBCConfig ([JDBCConfig configuration](configuration.md)), being sure to set `enabled`, `initdb`, and `import` to `true`, and to provide the connection information for an empty database.

7.  Start GeoServer again. This time JDBCConfig will connect to the specified database, initialize it, import the old catalog into it, and take over from the old catalog. Subsequent start ups will skip the initialize and import steps unless you re-enable them in `jdbcconfig.properties`.

8.  Log in as admin and a message should appear on the welcome page:

![image](h2message.png)
