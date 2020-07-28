.. _cite_test_guide:

Cite Test Guide
===============

A step by step guide to the GeoServer Compliance Interoperability Test Engine (CITE).

.. contents::
   :depth: 2

Requirements
------------

Running the tests requires a linux system with docker and docker-compose installed on it.

Check out CITE tools
--------------------

The CITE tools are available in the build/cite folder of the GeoServer repository:

https://github.com/geoserver/geoserver/tree/master/build/cite
  

Run the tests
-------------

.. note::
   The first Docker build may take a long time

#. Set environment variables

   Choose which test to run by setting the ETS environment variable::

    ETS=wcs10

   .. note::
      Valid values for the ETS parameter are
        * wcs10
        * wcs11
        * wfs10
        * wfs11
        * wms11
        * wms13
   
   Choose which GeoServer war to test by setting the GEOSERVER_WEBAPP_SRC environment variable::

    GEOSERVER_WEBAPP_SRC=https://build.geoserver.org/geoserver/master/geoserver-master-latest-war.zip

#. Build the testing suite::
  
    # FROM THE GEOSERVER CODE BASE ROOT FOLDER
    pushd build/cite
    docker_command="docker-compose -f docker-compose.yml -f ./${ETS}/docker-compose.override.yml "
    ### Cleaning up previous runs
    eval $docker_command  down --rmi all -v
    eval $docker_command  rm -vfs
    
    ### Build docker images
    eval $docker_command  build --build-arg "GEOSERVER_WEBAPP_SRC=${GEOSERVER_WEBAPP_SRC}" geoserver

#. Run the test::
  
    eval $docker_command  up --force-recreate teamengine

How to run TEAM Engine standalone
---------------------------------
To run a standalone version of TEAM Engine, start it with the following command::

    docker run -d --name standalone_teamengine -p 8080:8080 geosolutionsit/teamengine:latest

TEAM Engine will be accessible on http://localhost:8080/teamengine/

If you want to change the port, for example to have it on port "9090", change the command as follows::

    docker run -d --name standalone_teamengine -p 9090:8080 geosolutionsit/teamengine:latest

To stop TEAM Engine::

    docker stop standalone_teamengine
