---
render_macros: true
---

# Installing the Vector Tiles Extension

1.  From the [website download](https://geoserver.org/download) page, locate your release, and download: {{ download_extension('vectortiles') }} (nightly {{ download_extension('vectortiles','snapshot') }})

    !!! warning

        Make sure to match the version of the extension to the version of GeoServer.

2.  Extract the archive and copy the contents into the GeoServer **`WEB-INF/lib`** directory.

3.  Restart GeoServer.

To verify that the extension was installed successfully

1.  Open the [Web administration interface](../../webadmin/index.md)

2.  Click **Layers** and select a vector layer

3.  Click the **Tile Caching** tab

4.  Scroll down to the section on **Tile Formats**. In addition to the standard GIF/PNG/JPEG formats, you should see the following:

    -   `application/json;type=geojson`
    -   `application/json;type=topojson`
    -   `application/vnd.mapbox-vector-tile`

    ![](img/vectortiles_tileformats.png)
    *Vector tiles tile formats*

    If you don't see these options, the extension did not install correctly.
