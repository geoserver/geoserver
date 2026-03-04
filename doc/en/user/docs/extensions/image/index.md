---
render_macros: true
---

---
render_macros: true
---

# Installing the Image extension

The Image extension adds support for publishing raster images accompanied by an ESRI world file (a six-line text file used to georeference an image). World files are commonly named with a format-specific extension such as **`.pgw`** (PNG), **`.jgw`** (JPEG), or **`.tfw`** (TIFF); a generic **`.wld`** extension may also be used.

In GeoServer, this store type is referred to as **WorldImage**.

## Installing

To install the Image extension:

1.  Navigate to the [GeoServer download page](https://geoserver.org/download).

2.  Find the page that matches the exact version of GeoServer you are running.

    !!! warning

        Be sure to match the version of the extension with that of GeoServer, otherwise errors will occur.

3.  Download the Image extension:

    - {{ release }} [image](https://build.geoserver.org/geoserver/main/ext-latest/image)
    - {{ version }} [image](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ version }}-SNAPSHOT-image-plugin.zip)

    The download link for **Image** will be in the **Extensions** section.

4.  Stop GeoServer.

5.  Extract the contents of the archive into the **`WEB-INF/lib`** directory of your GeoServer installation.

    !!! note

        Ensure the JAR files are placed directly in the **`WEB-INF/lib`** directory and not in a subdirectory.

6.  Restart GeoServer.

## Verifying the installation

After restarting GeoServer:

1.  Navigate to **Stores** -> **Add new Store** and confirm that **WorldImage** is available under raster data sources.
2.  Navigate to **About & Status** -> **Status** and confirm that the Image module is listed under **Modules**.

## Using WorldImage

Once installed, configuring a WorldImage store is the same as configuring a standard WorldImage raster store.
