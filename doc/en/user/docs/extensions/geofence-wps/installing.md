---
render_macros: true
---

# Installing the GeoServer GeoFence WPS Integration

1.  Visit the [website download](https://geoserver.org/download) page, locate your release, and download: {{ download_extension('geofence-wps') }} (nightly {{ download_extension('geofence-wps','snapshot') }})

    The download link will be in the **Extensions** section under **Other**.

    !!! warning

        Ensure to match plugin (example {{ release }} above) version to the version of the GeoServer instance.

> 1.  Extract the files in this archive to the **`WEB-INF/lib`** directory of your GeoServer installation.
> 2.  Restart GeoServer
