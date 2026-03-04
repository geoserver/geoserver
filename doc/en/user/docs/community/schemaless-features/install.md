---
render_macros: true
---

---
render_macros: true
---

# Installing the Schemaless Mongo module

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download `mongodb-schemaless` zip archive.

    - {{ version }} example: [mongodb-schemaless](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ version }}-SNAPSHOT-mongodb-schemaless-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

    !!! warning

        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-{{ version }}-mongodb-schemaless-plugin.zip above).

4.  Restart GeoServer.

    On restart the `MongoDB Schemaless` vector source option is available from the `New Data Source` page:

    ![](images/new-data-sources-schemaless.png)
