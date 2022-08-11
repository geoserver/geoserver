.. _installation_docker:

Docker Container
================

Geoserver is also packaged as a Docker Container.  For more details, see the `Geoserver Docker Container Project <https://github.com/geoserver/docker>`_.

Quick Start
-----------

This will run the container with the data directory shipped with the container (which is typically an empty directory).

#. Make sure you have `Docker <https://www.docker.com/>`_ installed.
#. Download the container

   .. code-block:: 
    
      docker pull docker.osgeo.org/geoserver:2.21.1

#. Run the container

   .. code-block:: 
    
      docker run -it -p8080:8080 docker.osgeo.org/geoserver:2.21.1 
 
#. Visit `http://localhost:8080/geoserver <http://localhost:8080/geoserver>`_.

Using your own Data Directory
-----------------------------

This will run the container with a local data directory.  The data directory will be `mounted <https://docs.docker.com/storage/bind-mounts/>`_ into the docker container.

.. Note::

    Change `/MY/DATADIRECTORY` to your data directory.

#. Make sure you have `Docker <https://www.docker.com/>`_ installed.
#. Download the container
 
   .. code-block:: 
    
      docker pull docker.osgeo.org/geoserver:2.21.1

#. Run the container

   .. code-block:: 
    
      docker run \
           --mount type=bind,src=/MY/DATADIRECTORY,target=/opt/geoserver_data  \
           -it -p8080:8080 docker.osgeo.org/geoserver:2.21.1 

#. Visit `http://localhost:8080/geoserver <http://localhost:8080/geoserver>`_.

Using the Standard Geoserver Data Directory
-------------------------------------------

This will run the container with the standard (release) data directory, which includes a few sample layers.  
This Data Directory will be `mounted <https://docs.docker.com/storage/bind-mounts/>`_ into the docker container.

.. Note::

    Change `/MY/geoserver-2.21.1-data` to where your data directory is.

#. Make sure you have `Docker <https://www.docker.com/>`_ installed.
#. Download the container
 
   .. code-block:: 
    
      docker pull docker.osgeo.org/geoserver:2.21.1

#. Download the zipped Data Directory from `Source Forge <https://sourceforge.net/projects/geoserver/files/GeoServer/2.21.1/geoserver-2.21.1-data.zip/download>`_.

   .. Note::

      You can navigate to this by going to the `Geoserver Source Forge page <https://sourceforge.net/projects/geoserver/>`_ then to `files`, `Geoserver`, and then the release that matches your docker container.

#. Unzip the Data Directory

   .. code-block:: 
    
      unzip geoserver-2.21.1-data.zip

#. Run the container

   .. code-block:: 
    
      docker run \
           --mount type=bind,src=/MY/geoserver-2.21.1-data,target=/opt/geoserver_data  \
           -it -p8080:8080 docker.osgeo.org/geoserver:2.21.1 
        
#. Visit `http://localhost:8080/geoserver <http://localhost:8080/geoserver>`_.
