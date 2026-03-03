---
render_macros: true
---

# Installing the Monitor Micrometer Extension

1.  Visit the [website download](https://geoserver.org/download) page and download [monitor-micrometer](https://build.geoserver.org/geoserver/main/community-latest/monitor-micrometer).
2.  Extract the downloaded archive and copy the JAR files into the servlet container's `WEB-INF/lib` directory.
3.  Restart GeoServer.

!!! note

    For the module to work, the [Monitoring](../../extensions/monitoring/index.md) extension must also be installed.

## Installing the Monitor Micrometer Extension with Docker

To run the GeoServer Docker image with the Monitor Micrometer extension installed, use the following command:

> ::: admonition
> Release
> :::
>
> ``` text
> docker run -it -p8080:8080 \
>   --env INSTALL_EXTENSIONS=true \
>   --env STABLE_EXTENSIONS="monitor" \
>   --env COMMUNITY_EXTENSIONS="monitor-micrometer" \
>   docker.osgeo.org/geoserver:{{ release }}
> ```
>
> ::: admonition
> Nightly Build
> :::
>
> ``` text
> docker run -it -p8080:8080 \
>   --env INSTALL_EXTENSIONS=true \
>   --env STABLE_EXTENSIONS="monitor" \
>   --env COMMUNITY_EXTENSIONS="monitor-micrometer" \
>   docker.osgeo.org/geoserver:{{ version }}.x
> ```

If using GeoServer in Docker Compose, use this instead:

> ::: admonition
> Release
> :::
>
> ``` text
> services:
>   geoserver:
>     image: docker.osgeo.org/geoserver:{{ release }}
>     ports:
>       - "8080:8080"
>     environment:
>       INSTALL_EXTENSIONS: true
>       STABLE_EXTENSIONS: "monitor"
>       COMMUNITY_EXTENSIONS: "monitor-micrometer"
> ```
>
> ::: admonition
> Nightly Build
> :::
>
> ``` text
> services:
>   geoserver:
>     image: docker.osgeo.org/geoserver:{{ version }}.x
>     ports:
>       - "8080:8080"
>     environment:
>       INSTALL_EXTENSIONS: true
>       STABLE_EXTENSIONS: "monitor"
>       COMMUNITY_EXTENSIONS: "monitor-micrometer"
> ```
