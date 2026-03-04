---
render_macros: true
---

---
render_macros: true
---

# Installing the RAT module

To install the Raster Attribute Table support:

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Archive** tab, and locate your release.

    From the list of **Coverage Formats** extensions download **Raster Attribute Table**.

    - {{ release }} example: [rat](https://build.geoserver.org/geoserver/main/ext-latest/rat)
    - {{ version }} example: [rat](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ version }}-SNAPSHOT-rat-plugin.zip)

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

3.  Extract these files and place the JARs in `WEB-INF/lib`.

4.  Perform any configuration required by your servlet container, and then restart.
