.. _installation_docker:

Docker Container
================

Geoserver is also packaged as a Docker Container.  For more details, see the `Geoserver Docker Container Project <https://github.com/geoserver/docker>`__.

See the `README.md <https://github.com/geoserver/docker/blob/master/README.md>`__ file for more technical information.

Quick Start
-----------

This will run the container, with the data directory included with the container:

#. Make sure you have `Docker <https://www.docker.com/>`__ installed.
#. Download the container

    docker pull docker.osgeo.org/geoserver:|release|

#. Run the container

      docker run -it -p8080:8080 docker.osgeo.org/geoserver:|release|
 
#. In a web browser, navigate to ``http://localhost:8080/geoserver``.

   If you see the GeoServer Welcome page, then GeoServer is successfully installed.

   .. figure:: images/success.png

      GeoServer Welcome Page
      
#. This setup is a quick test to ensure the software is working, but is difficult to use as file data can only be transferred to the data directory included with the container via the REST API.

Using your own Data Directory
-----------------------------

This will run the container with a local data directory.  The data directory will be `mounted <https://docs.docker.com/storage/bind-mounts/>`__ into the docker container.

.. Note::

    Change `/MY/DATADIRECTORY` to your data directory.  If this directory is empty it will be populated with the standard Geoserver Sample Data Directory.

#. Make sure you have `Docker <https://www.docker.com/>`__ installed.

#. Download the container

    docker pull docker.osgeo.org/geoserver:|release|

#. Run the container

      docker run --mount type=bind,src=/MY/DATADIRECTORY,target=/opt/geoserver_data -it -p8080:8080 docker.osgeo.org/geoserver:|release|


#. In a web browser, navigate to ``http://localhost:8080/geoserver``.

   If you see the GeoServer Welcome page, then GeoServer is successfully installed.

   .. figure:: images/success.png

      GeoServer Welcome Page
      
#. This setup allows direct management of the file data shared with the container. This setup is also easy to update to use the latest container.

Adding Geoserver Extensions
---------------------------

You can add Geoserver Extensions - the container will download them during startup.

      docker run  -it -p8080:8080 --env INSTALL_EXTENSIONS=true --env STABLE_EXTENSIONS="ysld,h2" docker.osgeo.org/geoserver:|release|

This will download and install the YSLD and H2 extension.

Here is a list of available extensions (taken from the Geoserver download page):

::

    app-schema   gdal            jp2k          ogr-wps          web-resource
    authkey      geofence        libjpeg-turbo oracle           wmts-multi-dimensional
    cas          geofence-server mapml         params-extractor wps-cluster-hazelcast
    charts       geopkg-output   mbstyle       printing         wps-cluster-hazelcast
    control-flow grib            mongodb       pyramid          wps-download
    css          gwc-s3          monitor       querylayer       wps-jdbc
    csw          h2              mysql         sldservice       wps
    db2          imagemap        netcdf-out    sqlserver        xslt
    dxf          importer        netcdf        vectortiles      ysld
    excel        inspire         ogr-wfs       wcs2_0-eo

