---
render_macros: true
---


# YSLD Extension Installation

## Installing YSLD extension

The YSLD extension is listed on the GeoServer download page.

To install:

1.  Download:

    - {{ release }} [geoserver-{{ release }}-ysld-plugin.zip](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/extensions/geoserver-{{ release }}-ysld-plugin.zip)
    - {{ snapshot }} [geoserver-{{ snapshot }}-ysld-plugin.zip](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ snapshot }}-ysld-plugin.zip)

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

2.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer. Make sure you do not create any sub-directories during the extraction process.

3.  Restart GeoServer.

4.  To confirm successful installation, check for a new YSLD entry in the [Styles](../webadmin/index.md) editor.

## Docker use of YSLD extension

1.  The Docker image supports the use of YSLD extension

    !!! abstract "Release"


    ``` text
    docker pull docker.osgeo.org/geoserver:{{ release }}
    ```

    !!! abstract "Nightly Build"


    ``` text
    docker pull docker.osgeo.org/geoserver:{{ version }}.x
    ```

2.  To run with YSLD extension:

    !!! abstract "Release"


    ``` text
    docker run -it -p 8080:8080 \
      --env INSTALL_EXTENSIONS=true \
      --env STABLE_EXTENSIONS="ysld" \
      docker.osgeo.org/geoserver:{{ release }}
    ```

    !!! abstract "Nightly Build"


    ``` text
    docker run -it -p 8080:8080 \
      --env INSTALL_EXTENSIONS=true \
      --env STABLE_EXTENSIONS="ysld" \
      docker.osgeo.org/geoserver:{{ version }}.x
    ```

3.  To confirm successful installation, check for a new YSLD entry in the [Styles](../webadmin/index.md) editor.
