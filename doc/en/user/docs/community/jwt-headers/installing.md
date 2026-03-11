---
render_macros: true
---


# Installing JWT Headers

To install the JWT Headers module:

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download ``jwt-headers`` zip archive.

    - {{ snapshot }} example: [jwt-headers](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-jwt-headers-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

4.  Restart GeoServer.

Community module is are not yet ready for distribution with GeoServer release.

1.  To compile the JWT Headers community module yourself download the src bundle for your GeoServer version and compile:

    ``` bash
    cd src/community
    mvn install -PcommunityRelease -DskipTests
    ```

2.  And package:

    ``` bash
    cd src/community
    mvn assembly:single -N
    ```

3.  Place the JARs in `WEB-INF/lib`.

4.  Restart GeoServer.

For developers;

``` bash
cd src
mvn install -Pjwt-headers -DskipTests
```
