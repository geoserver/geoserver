---
render_macros: true
---

# Installing the GeoServer GeoFence Server extension {: #geofence_server_install }

1.  Visit the [website download](https://geoserver.org/download) page, locate your release, and download: `geofence-server`{.interpreted-text role="download_extension"}

    The download link will be in the **Extensions** section under **Other**.

    !!! warning

        Ensure to match plugin (example {{ release }} above) version to the version of the GeoServer instance.

> 1.  Extract the files in this archive to the **`WEB-INF/lib`** directory of your GeoServer installation.
>
>     ::: note
>     ::: title
>     Note
>     :::
>
>     By default GeoFence will store this data in a [H2 database](http://www.h2database.com/html/main.html) and the database schema will be automatically managed by Hibernate.
>
>     The [GeoFence documentation](https://github.com/geoserver/geofence/wiki/GeoFence-configuration) explains how to configure a different backed database and configure Hibernate behavior.
>     :::
>
> 2.  Add the following system variable among the JVM startup options (location varies depending on installation type): `-Dgwc.context.suffix=gwc`
>
> 3.  Restart GeoServer
