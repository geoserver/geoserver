# Installing the GeoServer MBTiles extension

!!! warning

    Make sure to match the version of the extension to the version of the GeoServer instance!

1.  Download the extensions from the [nightly GeoServer community module builds](https://build.geoserver.org/geoserver/main/community-latest/).

    > 1.  Download the `mbtiles-store-plugin` from `mbtiles-store`{.interpreted-text role="download_community"} if you simply want to read MBTiles files.
    > 2.  Download the `mbtiles-plugin` from `mbtiles`{.interpreted-text role="download_community"} if you also want to use the WMS output format generaring MBTiles and the WPS process doing the same. Make sure to install corresponding WPS extension for GeoServer instance before installing this plugin, or GeoServer won't start.

2.  Extract the contents of the archive into the `WEB-INF/lib` directory of the GeoServer installation.
