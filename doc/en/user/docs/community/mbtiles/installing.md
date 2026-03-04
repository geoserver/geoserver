---
render_macros: true
---

---
render_macros: true
---

# Installing the GeoServer MBTiles extension

!!! warning

    Make sure to match the version of the extension to the version of the GeoServer instance!

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

3.  Follow the **Community Modules** link:

    Download the `mbtiles-store-plugin` if you wish to read MBTiles

    - {{ version }} example: [mbtiles-store](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ version }}-SNAPSHOT-mbtiles-store-plugin.zip)

    Download the `mbtiles-plugin` to also use the WMS output format generaring MBTiles and the WPS process doing the same. Make sure to install corresponding WPS extension for GeoServer instance before installing this plugin, or GeoServer won't start.

    - {{ version }} example: [mbtiles](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ version }}-SNAPSHOT-mbtiles-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

4.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

5.  Restart GeoServer.
