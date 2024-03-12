---
render_macros: true
---

# Installing JWT Headers

To install the JWT Headers module:

1.  If working with a {{ version }}.x nightly build, download the module: {{ download_community('jwt-headers','snapshot') }}

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ version }}.x above).

2.  Place the JARs in `WEB-INF/lib`.

3.  Restart GeoServer.

!!! note

    Community modules are not yet ready for distribution with GeoServer {{ release }} release.
    
    To compile the JWT Headers community module yourself download the src bundle for GeoServer {{ release }} and compile:
    
    ``` bash
    ```
    
    cd src/community mvn install -PcommunityRelease -DskipTests
    
    And package:
    
    ``` bash
    ```
    
    cd src/community mvn assembly:single -N

For developers;

``` bash
cd src
mvn install -Pjwt-headers -DskipTests
```
