---
render_macros: true
---


# Installing the GeoServer GeoFence WPS Integration

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Archive** tab, and locate your release.

    From the list of **Security** extensions download **GeoFence WPS**.

    - {{ release }} example: [geofence-wps](https://build.geoserver.org/geoserver/main/ext-latest/geofence-wps)
    - {{ version }} example: [geofence-wps](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ version }}-SNAPSHOT-geofence-wps-plugin.zip)

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

> 1.  Extract the files in this archive to the **`WEB-INF/lib`** directory of your GeoServer installation.
> 2.  Restart GeoServer
