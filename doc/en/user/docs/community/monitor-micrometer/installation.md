---
render_macros: true
---

---
render_macros: true
---

# Installing the Monitor Micrometer Extension

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  For the module to work, the [Monitoring](../../extensions/monitoring/index.md) extension must also be installed.

    Visit the [website download](https://geoserver.org/download) page, change the **Archive** tab, and locate your release.

    From the list of **Miscellaneous** extensions download **Monitor (Core)**.

    - {{ release }} example: [monitor](https://build.geoserver.org/geoserver/main/ext-latest/monitor)
    - {{ version }} example: [monitor](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ version }}-SNAPSHOT-monitor-plugin.zip)

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

3.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download ``monitor-micrometer`` zip archive.

    - {{ version }} example: [monitor-micrometer](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ version }}-SNAPSHOT-monitor-micrometer-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

4.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

5.  Restart GeoServer.

## Installing the Monitor Micrometer Extension with Docker

To run the GeoServer Docker image with the Monitor Micrometer extension installed, use the following command:

{{ release }} example:

``` text
docker run -it -p 8080:8080 \
  --env INSTALL_EXTENSIONS=true \
  --env STABLE_EXTENSIONS="monitor" \
  --env COMMUNITY_EXTENSIONS="monitor-micrometer" \
  docker.osgeo.org/geoserver:{{ release }}
```

{{ version }} example:

``` text
docker run -it -p 8080:8080 \
  --env INSTALL_EXTENSIONS=true \
  --env STABLE_EXTENSIONS="monitor" \
  --env COMMUNITY_EXTENSIONS="monitor-micrometer" \
  docker.osgeo.org/geoserver:{{ version }}.x
```

If using GeoServer in Docker Compose, use this instead:

{{ release }} example:

``` text
services:
  geoserver:
    image: docker.osgeo.org/geoserver:{{ release }}
    ports:
      - "8080:8080"
    environment:
      INSTALL_EXTENSIONS: true
      STABLE_EXTENSIONS: "monitor"
      COMMUNITY_EXTENSIONS: "monitor-micrometer"
```

{{ version }} example:

``` text
services:
  geoserver:
    image: docker.osgeo.org/geoserver:{{ version }}.x
    ports:
      - "8080:8080"
    environment:
      INSTALL_EXTENSIONS: true
      STABLE_EXTENSIONS: "monitor"
      COMMUNITY_EXTENSIONS: "monitor-micrometer"
```
