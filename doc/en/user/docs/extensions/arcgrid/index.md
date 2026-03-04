---
render_macros: true
---

---
render_macros: true
---

# Installing the ArcGrid extension

The ArcGrid extension adds support for publishing ESRI ArcGrid raster datasets as coverages.

ArcGrid support is provided as an optional GeoServer extension and must be installed separately.

## Installing

To install the ArcGrid extension:

1.  Navigate to the [GeoServer download page](https://geoserver.org/download).

2.  Find the page that matches the exact version of GeoServer you are running.

    !!! warning

        Be sure to match the version of the extension with that of GeoServer, otherwise errors will occur.

3.  Download the ArcGrid extension:

    - {{ release }} [arcgrid](https://build.geoserver.org/geoserver/main/ext-latest/arcgrid)
    - {{ version }} [arcgrid](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ version }}-SNAPSHOT-arcgrid-plugin.zip)

    The download link for **ArcGrid** will be in the **Extensions** section.

4.  Stop GeoServer.

5.  Extract the contents of the archive into the **`WEB-INF/lib`** directory of your GeoServer installation.

    !!! note

        Ensure the JAR files are placed directly in the **`WEB-INF/lib`** directory and not in a subdirectory.

6.  Restart GeoServer.

## Verifying the installation

After restarting GeoServer:

1.  Navigate to **Stores** -> **Add new Store** and confirm that **ArcGrid** is available under raster data sources.
2.  Navigate to **About & Status** -> **Status** and confirm that the ArcGrid module is listed under **Modules**.

## Using ArcGrid

Once installed, configuring an ArcGrid store is the same as configuring a standard ArcGrid raster store.
