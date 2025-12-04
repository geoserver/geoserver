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
        --env STABLE_EXTENSIONS="ysld,ogcapi-features" \\
        docker.osgeo.org/geoserver:|release|

.. only:: snapshot

   .. parsed-literal::

      docker run -it -p8080:8080 \\
        --env INSTALL_EXTENSIONS=true \\
        --env STABLE_EXTENSIONS="ysld,ogcapi-features" \\
        docker.osgeo.org/geoserver:|version|.x


This will download and install the :ref:`YSLD <ysld_styling>` and :ref:`OGCAPI - Features <ogcapi-features>` extension.

Here is a list of available extensions (taken from the `build server <https://build.geoserver.org/geoserver/main/ext-latest/>`__):

::

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


Testing Geoserver Community modules
-----------------------------------

Working with a Nightly build is a good way to test community modules and provide feedback to developers working on new functionality.

.. only:: not snapshot

   Community modules are shared as part GeoServer |release| source code bundle to be compiled for testing
   and feedback by the developer community.
   
   When the developer has met the documentation and quality assurance standards for GeoServer they may
   ask for the module to be included in GeoServer.
   
   If you are interested in helping out, please make contact via the `developer forum <https://discourse.osgeo.org/c/geoserver/developer/63>`__.
   
   Reference:
   
   * :developer:`community modules <policies/community-modules.html>` (Developer Guide)
      
   
.. only:: snapshot
   
   To work with community modules you must be using the GeoServer |version|.x nightly build that matches the community module build:
   
   .. parsed-literal::
   
      docker run -it -p8080:8080 \\
        --env INSTALL_EXTENSIONS=true \\
        --env STABLE_EXTENSIONS="ysld,h2" \\
        --env COMMUNITY_EXTENSIONS="ogcapi-images,ogcapi-maps,ogcapi-styles,ogcapi-tiles" \\
        docker.osgeo.org/geoserver:|version|.x
   
   For the current list see GeoServer `build server <https://build.geoserver.org/geoserver/main/community-latest/>`__.
   
   ::
   
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
