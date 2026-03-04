---
render_macros: true
---


# Docker Container

Geoserver is also packaged as a Docker Container. For more details, see the [Geoserver Docker Container Project](https://github.com/geoserver/docker).

See the [README.md](https://github.com/geoserver/docker/blob/master/README.md) file for more technical information.

## Quick Start

This will run the container, with the data directory included with the container:

1.  Make sure you have [Docker](https://www.docker.com/) installed.

2.  Download the container:

    !!! abstract "Release"


    These instructions are for GeoServer {{ release }}.

    ``` text
    docker pull docker.osgeo.org/geoserver:{{ release }}
    ```

    !!! abstract "Nightly Build"


    These instructions are for GeoServer {{ version }}-SNAPSHOT which is provided as a [Nightly](https://geoserver.org/release/main) release. Testing a Nightly release is a great way to try out new features, and test community modules. Nightly releases change on an ongoing basis and are not suitable for a production environment.

    ``` text
    docker pull docker.osgeo.org/geoserver:{{ version }}.x
    ```

3.  Run the container

    !!! abstract "Release"


    ``` text
    docker run -it -p 8080:8080 docker.osgeo.org/geoserver:{{ release }}
    ```

    !!! abstract "Nightly Build"


    ``` text
    docker run -it -p 8080:8080 docker.osgeo.org/geoserver:{{ version }}.x
    ```

4.  In a web browser, navigate to `http://localhost:8080/geoserver`.

    If you see the GeoServer Welcome page, then GeoServer is successfully installed.

    ![](images/success.png)
    *GeoServer Welcome Page*

5.  This setup is a quick test to ensure the software is working, but is difficult to use as file data can only be transferred to the data directory included with the container via the REST API.

## Using your own Data Directory {: #installation_docker_data }

This will run the container with a local data directory. The data directory will be [mounted](https://docs.docker.com/storage/bind-mounts/) into the docker container.

!!! note

    Change ``/MY/DATADIRECTORY`` to your data directory. If this directory is empty it will be populated with the standard Geoserver Sample Data Directory.

1.  Make sure you have [Docker](https://www.docker.com/) installed.

2.  Download the container

    !!! abstract "Release"


    ``` text
    docker pull docker.osgeo.org/geoserver:{{ release }}
    ```

    !!! abstract "Nightly Build"


    ``` text
    docker pull docker.osgeo.org/geoserver:{{ version }}.x
    ```

3.  Run the container

    !!! abstract "Release"


    ``` text
    docker run  -it -p 8080:8080 \
      --mount type=bind,src=/MY/DATADIRECTORY,target=/opt/geoserver_data \
      docker.osgeo.org/geoserver:{{ release }}
    ```

    !!! abstract "Nightly Build"


    ``` text
    docker run -it -p 8080:8080 \
      --mount type=bind,src=/MY/DATADIRECTORY,target=/opt/geoserver_data \
      docker.osgeo.org/geoserver:{{ version }}.x
    ```

4.  In a web browser, navigate to `http://localhost:8080/geoserver`.

    If you see the GeoServer Welcome page, then GeoServer is successfully installed.

    ![](images/success.png)
    *GeoServer Welcome Page*

5.  This setup allows direct management of the file data shared with the container. This setup is also easy to update to use the latest container.

## Adding GeoServer Extensions

You can add GeoServer Extensions - the container will download them during startup.

!!! abstract "Release"


``` text
docker run -it -p 8080:8080 \
  --env INSTALL_EXTENSIONS=true \
  --env STABLE_EXTENSIONS="ysld,ogcapi-features" \
  docker.osgeo.org/geoserver:{{ release }}
```

!!! abstract "Nightly Build"


``` text
docker run -it -p 8080:8080 \
  --env INSTALL_EXTENSIONS=true \
  --env STABLE_EXTENSIONS="ysld,ogcapi-features" \
  docker.osgeo.org/geoserver:{{ version }}.x
```

This will download and install the [YSLD](../styling/ysld/index.md) and [OGCAPI - Features](../services/features/index.md) extension.

Here is a list of available extensions (taken from the [build server](https://build.geoserver.org/geoserver/main/ext-latest/)):

    app-schema               geopkg-output            ogr-wps
    authkey                  grib                     oracle
    cas                      gwc-s3                   params-extractor
    charts                   iau                      printing                 
    control-flow             importer                 pyramid                  
    css                      inspire                  querylayer               
    csw-iso                  jp2k                     rat                      
    csw                      libjpeg-turbo            sldservice
    datadir-catalog-loader   mapml                    sqlserver
    db2                      mbstyle                  vectortiles
    dxf                      metadata                 wcs2_0-eo
    excel                    mongodb                  web-resource
    feature-pregeneralized   monitor                  wmts-multi-dimensional
    gdal                     mysql                    wps-cluster-hazelcas
    geofence                 netcdf-out               wps-download
    geofence-server-h2       netcdf                   wps-jdbc
    geofence-server-postgres ogcapi-features          wps
    geofence-wps             ogr-wfs                  ysld
    kml

## Testing Geoserver Community modules

Working with a Nightly build is a good way to test community modules and provide feedback to developers working on new functionality.

!!! abstract "Release"


Community modules are shared as part GeoServer {{ release }} source code bundle to be compiled for testing and feedback by the developer community.

When the developer has met the documentation and quality assurance standards for GeoServer they may ask for the module to be included in GeoServer.

If you are interested in helping out, please make contact via the [developer forum](https://discourse.osgeo.org/c/geoserver/developer/63).

Reference:

- [community modules](https://docs.geoserver.org/latest/en/developer/policies/community-modules.md) (Developer Guide)

!!! abstract "Nightly Build"


To work with community modules you must be using the GeoServer {{ version }}.x nightly build that matches the community module build:

``` text
docker run -it -p 8080:8080 \
  --env INSTALL_EXTENSIONS=true \
  --env STABLE_EXTENSIONS="ysld,h2" \
  --env COMMUNITY_EXTENSIONS="ogcapi-images,ogcapi-maps,ogcapi-styles,ogcapi-tiles" \
  docker.osgeo.org/geoserver:{{ version }}.x
```

For the current list see GeoServer [build server](https://build.geoserver.org/geoserver/main/community-latest/).

    acl                           gwc-mbtiles                     ogcapi-tiles
    activeMQ-broker               gwc-sqlite                      ogr-datastore
    backup-restore                hz-cluster                      opensearch-eo
    cog-azure                                                     proxy-base-ext
    cog-google                    importer-jdbc                   s3-geotiff
    cog-http                                                      sec-keycloak
    cog-s3                        jdbcconfig                      sec-oauth2-geonode
    colormap                      jdbcstore                       sec-oauth2-github
    cov-json                      jms-cluster                     sec-oauth2-google
                                  libdeflate                      sec-oauth2-openid
    dds                           mbtiles                         smart-data-loader
    elasticsearch                 mbtiles-store                   solr
    features-autopopulate         mongodb-schemaless              spatialjson
    features-templating           monitor-kafka                   stac-datastore
    flatgeobuf                    ncwms                           taskmanager-core
    gdal-wcs                      netcdf-ghrsst                   taskmanager-s3
    gdal-wps                      notification                    vector-mosaic
    geopkg                        ogcapi-coverages                vsi
    gpx                           ogcapi-dggs                     webp
    graticule                     ogcapi-images                   wfs-freemarker
    gsr                           ogcapi-maps                     wps-longitudinal-profile
    gwc-azure-blobstore           ogcapi-styles                   wps-remote
                                  ogcapi-tiled-features               
