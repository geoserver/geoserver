.. _installation_docker:

Docker Container
================

Geoserver is also packaged as a Docker Container.  For more details, see the `Geoserver Docker Container Project <https://github.com/geoserver/docker>`_.

Quick Start
-----------

This will run the container with the data directory shipped with the container (which is typically an empty directory).

#. Make sure you have `Docker <https://www.docker.com/>`__ installed.
#. Download the container

    docker pull docker.osgeo.org/geoserver:|release|

#. Run the container

      docker run -it -p8080:8080 docker.osgeo.org/geoserver:|release|
 
#. Visit  http://localhost:8080/geoserver.

Using your own Data Directory
-----------------------------

This will run the container with a local data directory.  The data directory will be `mounted <https://docs.docker.com/storage/bind-mounts/>`__ into the docker container.

.. Note::

    Change `/MY/DATADIRECTORY` to your data directory.

#. Make sure you have `Docker <https://www.docker.com/>`__ installed.
#. Download the container

    docker pull docker.osgeo.org/geoserver:|release|

#. Run the container

      docker run \-\-mount type=bind,src=/MY/DATADIRECTORY,target=/opt/geoserver_data -it -p8080:8080 docker.osgeo.org/geoserver:|release|


#. Visit  http://localhost:8080/geoserver.

Using the Standard Geoserver Data Directory
-------------------------------------------

This will run the container with the standard (release) data directory, which includes a few sample layers.  
This Data Directory will be `mounted <https://docs.docker.com/storage/bind-mounts/>`__ into the docker container.


#. Make sure you have `Docker <https://www.docker.com/>`__ installed.
#. Download the container

    docker pull docker.osgeo.org/geoserver:|release|

#. Download the Release Geoseoserver Data Directory :download_release:`data`

#. Unzip the Data Directory (this will create a directory called `geoserver_data`)

    unzip geoserver-\*-data.zip

#. Run the container

      docker run \-\-mount type=bind,src=`pwd`/geoserver_data,target=/opt/geoserver_data -it -p8080:8080 docker.osgeo.org/geoserver:|release|
        
#. Visit  http://localhost:8080/geoserver.


