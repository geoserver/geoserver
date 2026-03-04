---
render_macros: true
---

---
render_macros: true
---

# Installing the GeoServer FEATURES-TEMPLATING extension

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download ``features-templating`` zip archive.

    - {{ version }} example: [features-templating](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ version }}-SNAPSHOT-features-templating-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

4.  The full package requires the OGC API - Features service to be available. If the server does not include it, the jar `gs-features-templating-ogcapi-<version>.jar` should be removed from `WEB-INF/lib`

5.  Restart GeoServer.
