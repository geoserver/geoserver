.. _installation_docker:

Docker Container
================

Geoserver is also packaged as a Docker Container.  For more details, see the `Geoserver Docker Container Project <https://github.com/geoserver/docker>`__.

See the `README.md <https://github.com/geoserver/docker/blob/master/README.md>`__ file for more technical information.

Quick Start
-----------

This will run the container, with the data directory included with the container:

#. Make sure you have `Docker <https://www.docker.com/>`__ installed.

#. Download the container:

   .. only:: not snapshot

      These instructions are for GeoServer |release|.
      
      .. parsed-literal::
         
         docker pull docker.osgeo.org/geoserver:|release|

   .. only:: snapshot
      
      These instructions are for GeoServer |version|-SNAPSHOT which is provided as a :website:`Nightly <release/main>` release.
      Testing a Nightly release is a great way to try out new features, and test community modules.
      Nightly releases change on an ongoing basis and are not suitable for a production environment.
   
      .. parsed-literal::
         
         docker pull docker.osgeo.org/geoserver:|version|.x

#. Run the container

   .. only:: not snapshot
   
      .. parsed-literal::

         docker run -it -p8080:8080 docker.osgeo.org/geoserver:|release|

   .. only:: snapshot
   
      .. parsed-literal::

         docker run -it -p8080:8080 docker.osgeo.org/geoserver:|version|.x

 
#. In a web browser, navigate to ``http://localhost:8080/geoserver``.

   If you see the GeoServer Welcome page, then GeoServer is successfully installed.

   .. figure:: images/success.png

      GeoServer Welcome Page
      
#. This setup is a quick test to ensure the software is working, but is difficult to use as file data can only be transferred to the data directory included with the container via the REST API.

.. _installation_docker_data:

Using your own Data Directory
-----------------------------

This will run the container with a local data directory.  The data directory will be `mounted <https://docs.docker.com/storage/bind-mounts/>`__ into the docker container.

.. note::

    Change `/MY/DATADIRECTORY` to your data directory.  If this directory is empty it will be populated with the standard Geoserver Sample Data Directory.

#. Make sure you have `Docker <https://www.docker.com/>`__ installed.

#. Download the container

   .. only:: not snapshot
   
      .. parsed-literal::

         docker pull docker.osgeo.org/geoserver:|release|

   .. only:: snapshot
   
      .. parsed-literal::
   
         docker pull docker.osgeo.org/geoserver:|version|.x

#. Run the container

   .. only:: not snapshot

      .. parsed-literal::
         
         docker run  -it -p8080:8080 \\
           --mount type=bind,src=/MY/DATADIRECTORY,target=/opt/geoserver_data \\
           docker.osgeo.org/geoserver:|release|
      
   .. only:: snapshot
   
      .. parsed-literal::
         
         docker run -it -p8080:8080 \\
           --mount type=bind,src=/MY/DATADIRECTORY,target=/opt/geoserver_data \\
           docker.osgeo.org/geoserver:|version|.x

#. In a web browser, navigate to ``http://localhost:8080/geoserver``.

   If you see the GeoServer Welcome page, then GeoServer is successfully installed.

   .. figure:: images/success.png

      GeoServer Welcome Page
      
#. This setup allows direct management of the file data shared with the container. This setup is also easy to update to use the latest container.

Adding GeoServer Extensions
---------------------------

You can add GeoServer Extensions - the container will download them during startup.

.. only:: not snapshot

   .. parsed-literal::
   
      docker run -it -p8080:8080 \\
        --env INSTALL_EXTENSIONS=true \\
        --env STABLE_EXTENSIONS="ysld,h2" \\
        docker.osgeo.org/geoserver:|release|

.. only:: snapshot

   .. parsed-literal::

      docker run -it -p8080:8080 \\
        --env INSTALL_EXTENSIONS=true \\
        --env STABLE_EXTENSIONS="ysld,h2" \\
        docker.osgeo.org/geoserver:|version|.x


This will download and install the YSLD and H2 extension.

Here is a list of available extensions (taken from the `build server <https://build.geoserver.org/geoserver/main/ext-latest/>`__):

::

    app-schema   gdal            jp2k          ogr-wps          web-resource
    authkey      geofence        libjpeg-turbo oracle           wmts-multi-dimensional
    cas          geofence-server mapml         params-extractor wps-cluster-hazelcast
    charts       geopkg-output   mbstyle       printing         wps-download
    control-flow grib            mongodb       pyramid          wps-jdbc
    css          gwc-s3          monitor       querylayer       wps
    csw          h2              mysql         sldservice       
    db2                          netcdf-out    sqlserver        ysld
    dxf          importer        netcdf        vectortiles      
    excel        inspire         ogr-wfs       wcs2_0-eo

Testing Geoserver Community modules
-----------------------------------

Working with a Nightly build is a good way to test community modules and provide feedback to developers working on new functionality.

.. only:: not snapshot

   Community modules are shared as part GeoServer |release| source code bundle to be compiled for testing
   and feedback by the developer community.
   
   When the developer has met the documentation and quality assurance standards for GeoServer they may
   ask for the module to be included in GeoServer.
   
   If you are interested in helping out please contact the developer (list in the :file:`pom.xml` file for the module).
   
   Reference:
   
   * :developer:`community modules <policies/community-modules.html>` (Developer Guide)
      
   
.. only:: snapshot
   
   To work with community modules you must be using the GeoServer |version|.x nightly build that matches the community module build:
   
   .. parsed-literal::
   
      docker run -it -p8080:8080 \\
        --env INSTALL_EXTENSIONS=true \\
        --env STABLE_EXTENSIONS="ysld,h2" \\
        --env COMMUNITY_EXTENSIONS="ogcapi-features,ogcapi-images,ogcapi-maps,ogcapi-styles,ogcapi-tiles" \\
        docker.osgeo.org/geoserver:|version|.x
   
   For the current list see GeoServer `build server <https://build.geoserver.org/geoserver/main/community-latest/>`__.
   
   ::
   
    activeMQ-broker            jdbcconfig                 proxy-base-ext
    backup-restore             jdbcstore                  s3-geotiff
    cog                        jms-cluster                sec-keycloak
    colormap                   libdeflate                 sec-oauth2-geonode
    cov-json                   mbtiles                    sec-oauth2-github
    dds                        mbtiles-store              sec-oauth2-google
    dyndimension               mongodb-schemaless         sec-oauth2-openid-connect
    elasticsearch              ncwms                      smart-data-loader
    features-templating        netcdf-ghrsst              solr
    flatgeobuf                 notification               spatialjson
    gdal-wcs                   ogcapi-coverages           stac-datastore
    gdal-wps                   ogcapi-dggs                taskmanager-core
    geopkg                     ogcapi-features            taskmanager-s3
    gpx                        ogcapi-images              vector-mosaic
    gsr                        ogcapi-maps                vsi
    gwc-azure-blobstore        ogcapi-styles              webp
    gwc-mbtiles                ogcapi-tiled-features      wps-remote
    gwc-sqlite                 ogcapi-tiles
    hz-cluster                 ogr-datastore
    importer-jdbc              opensearch-eo


    
