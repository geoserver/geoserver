---
render_macros: true
---

---
render_macros: true
---

# Installing the OAUTH2/OIDC module

To install the OIDC module:

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download `sec-oidc` zip archive.

    - {{ version }} example: [sec-oidc](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ version }}-SNAPSHOT-sec-oidc-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

    !!! warning

        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-{{ version }}-loader-plugin.zip above).

4.  Restart GeoServer.

Community modules are not yet ready for distribution with GeoServer release.

1.  To compile the OIDC module yourself download the src bundle for your GeoServer version and compile:

    ``` bash
    cd src/community
    mvn install -PcommunityRelease -DskipTests
    ```

2.  And package (from the top level geoserver directory):

    ``` bash
    cd ../..
    mvn -f src/community/pom.xml clean install -B -DskipTests -PcommunityRelease,assembly  -T2 -fae
    ```

3.  Place the JARs in `WEB-INF/lib`.

4.  Restart GeoServer.

## Using with the GeoServer Docker Container

This will run GeoServer on port 9999 and install the OIDC module.

``` bash
docker run -it -p 9999:8080 \
   --env INSTALL_EXTENSIONS=true \
   --env STABLE_EXTENSIONS="ysld,h2" \
   --env COMMUNITY_EXTENSIONS="sec-oidc-plugin" \
   --env PROXY_BASE_URL="http://localhost:9999/geoserver" \
   docker.osgeo.org/geoserver:2.28.x
```

!!! note

    Setting `PROXY_BASE_URL` ensures that the OIDC [Redirect Base URI](configuring.md#community_oidc_redirect_base_uri) is correctly resolved to the external URL that users access. Without it, GeoServer may use an internal Docker hostname for the redirect URI, causing authentication to fail.

If your OIDC IDP server (i.e. keycloak) is running on ``localhost``, then you should ensure that all requests to the IDP occur using the same hostname (this includes the local user's browser and GeoServer directly connecting to the IDP). If you are running your IDP from a real host, then you do NOT have to do this;

> 1.  Add this to your `/etc/hosts`:
>
>     ``` bash
>     127.0.0.1       host.docker.internal
>     ```
>
> 2.  In your GeoServer OIDC configuration, use ``host.docker.internal`` instead of ``localhost``
>
> 3.  Access GeoServer and Keycloak with [http://host.docker.internal:PORT](http://host.docker.internal:PORT)
