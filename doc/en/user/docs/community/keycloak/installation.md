---
render_macros: true
---

# Installing Keycloak

To install the keycloak module:

1.  To obtain the keycloak community module:

    -   If working with a {{ release }} nightly build, download the module: {{ download_community('keycloak','snapshot') }}

        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

    -   Community modules are not yet ready for distribution with GeoServer release.

        To compile the keycloak community module yourself download the src bundle for your GeoServer version and compile:

        ``` bash
        ```

    cd src/community mvn install -PcommunityRelease -DskipTests

    > And package:
    >
    > ``` bash
    > ```

    cd src/community mvn assembly:single -N

2.  Place the JARs in `WEB-INF/lib`.

3.  Restart GeoServer.
