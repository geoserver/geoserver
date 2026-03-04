---
render_macros: true
---

---
render_macros: true
---

# Installing the GeoServer GeoFence extension

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Archive** tab, and locate your release.

    From the list of **Security** extensions download **GeoFence Client**.

    - {{ release }} example: [geofence](https://build.geoserver.org/geoserver/main/ext-latest/geofence)
    - {{ version }} example: [geofence](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ version }}-SNAPSHOT-geofence-plugin.zip)

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

3.  Extract the files in this archive to the **`WEB-INF/lib`** directory of your GeoServer installation.

4.  Restart GeoServer
