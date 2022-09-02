.. _cite_test_guide:

Cite Test Guide
===============

A step by step guide to the GeoServer Compliance Interoperability Test Engine (CITE).

.. contents::

~~~~~~~~~~~~~


Check out CITE suite tests
--------------------------

.. note:: The CITE suite tests are available at `Open Geospatial Consortium`_.
.. _Open Geospatial Consortium: https://github.com/opengeospatial

Requirements:
-------------

- `GeoServer instance <https://github.com/geosolutions-it/geoserver>`_.

- `Teamengine Web Application <https://github.com/geosolutions-it/teamengine-docker>`_, with a set of CITE suite tests.

- make


CITE automation tests with docker
=================================


How to run the CITE Test suites with
`docker <https://www.docker.com>`_.

Requirements:
-------------

- Running the tests requires a linux system with `docker <https://www.docker.com>`_, `docker-compose <https://docs.docker.com/compose/install>`_, and git installed on it.

.. note::

   The CITE tools are available in the build/cite folder of the `GeoServer Git repository <https://github.com/geoserver/geoserver/tree/master/build/cite>`_:

Steps:
------

Set-up the environment.
~~~~~~~~~~~~~~~~~~~~~~~

   #.  Clone the repository.

       .. code:: shell

          git clone https://github.com/geoserver/geoserver.git

   #.  Go to cite directory.

       .. code:: shell

          cd geoserver/build/cite

   #.  Inside you will find a structure, like below, with a list of directories which contains the name of the suites to run.

       .. code:: shell

          cite
          |-- forms
          |-- geoserver
          |-- run-test.sh
          |-- wcs10
          |-- wcs11
          |-- wfs10
          |-- wms11
          |-- wms13
          |-- wfs11
          |-- interactive
          |-- logs
          |-- docker-compose.yml
          |-- postgres
          |-- README.md
          `-- Makefile

Running the suite tests.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   There are 2 ways to run the suites. One is running with ``make`` that will
   automate all the commands, and the second one is running the test through WebUI:

   1. Running it through ``Makefile``:

      -  run ``make`` in the console, it will give you the list of commands
         to run.

         .. code:: shell

            make

      -  the output will like this:

         .. code:: makefile

            clean: $(suite)         Will Clean the Environment of previous runs.
            build: $(suite)         Will Build the GeoServer Docker Image for the Environment.
            test: $(suite)      Will running the Suite test with teamengine.
            webUI: $(suite)		 Will running the Suite test with teamengine.

      - Choose which test to run, this is an example:

        .. warning::

            The first Docker build may take a long time.

        .. code:: SHELL

           suite=wcs10

        .. note::

           Valid values for the suite parameter are:
             * wcs10
             * wcs11
             * wfs10
             * wfs11
             * wms11
             * wms13

      - Choose which GeoServer war file to test by setting the ``war_url`` environment variable inside the ``Makefile``, ex:

        .. code:: C

          war_url = "https://build.geoserver.org/geoserver/main/geoserver-main-latest-war.zip"

      .. note::

        if you don't want to do it inside the ``Makefile`` you have the option of adding the variable in the command when you build the docker images.

      -  To clean the local environment.

         .. code:: shell

            make clean

      -  To build the geoserver docker image locally.

         .. code:: shell

            make build suite=<suite-name>

      - Alternative, with the ``war_url`` variable include:

         .. code::

           make build suite=<suite-name> war_url=<url-or-the-geoserver-war-file-desired>

      -  To run the suite test.

         .. code:: shell

            make test suite=<suite-name>

      -  To run the full automate workflow.


         .. code:: shell

            make clean build test suite=<suite-name>


Run CITE Test Suites in local pc
================================

.. note::

   I assume that you have a standalone geoserver running.

.. important::

   Details to consider when you are running the tests:

   - The Default username/password for the teamengine webUI are **teamengine/teamengine**.

   - the default url for the teamengine webUI is http://localhost:8888/teamengine/

   - The output of the old suite tests might not appear in the Result page. So you should click on the link below **detailed old test report**, to get the full report. Ex.

   .. image:: ./image/old-report.png

   .. image:: ./image/full-report.png

   - Since you are running teamengine inside a container, the localhost in the url of geoserver for the tests can't be used, for that, get the ip of host where the geoserver is running. You will use it later.

   - after you log in to teamengine webUI you have to create a session.

   .. image:: ./image/seccion.png

   - to run the tests you have to choose which one you want, and then click on **Start a new test session**. This is an example:

   .. image:: ./image/tewfs-1_0a.png




Requirements:
-------------

- GeoServer running.

- PostgreSQL with PostGIS extension installed. (only for the WFS Tests Suites)

- Teamengine Running in docker container.



#. Clone the repository:

   .. code:: shell

      git clone https://github.com/geoserver/geoserver.git

#. Change directory to the ``cite``

   .. code:: shell

      cd geoserver/build/cite

#. Check the commands available:

   - Run ``make`` to check:

   .. code:: shell

        make


   - you should get an output as following:

   .. code:: makefile

        clean: $(suite)		 Will Clean the Environment of previous runs.
        build: $(suite)		 Will Build the GeoServer Docker Image for the Environment.
        test: $(suite)		 Will running the Suite test with teamengine.
        webUI: $(suite)		 Will running the Suite test with teamengine.


Run WFS 1.0 tests
-----------------

.. important::

   Running WFS 1.0 tests require PostgreSQL with PostGIS extension installed in the system.

Requirements:
~~~~~~~~~~~~~

- `GeoServer running`
- teamengine
- PostgreSQL
- PostGIS

#. Prepare the environment:

   - login to postgresql and create a user named "cite".

   .. code:: sql

     createuser cite;

   - Create a database named "cite", owned by the "cite" user:

   .. code:: sql

     createdb cite own by cite;

   - enter to the database and enable the postgis extension:

   .. code:: sql

    create extension postgis;

   - Change directory to the citewfs-1.0 data directory and execute the script cite_data_postgis2.sql:

   .. code-block:: shell

    cd <path of geoserver repository>
    psql -U cite cite < build/cite/wfs10/citewfs-1.0/cite_data_postgis2.sql

   - Start GeoServer with the citewfs-1.0 data directory. Example:

   .. important::

     If the postgresql server is not in the same host of the geoserver, you have to change the `<entry key="host">localhost</entry>` in the `datastore.xml` file, located inside each workspace directory. ex.

     .. note::

       <path of geoserver repository>/build/cite/wfs10/citewfs-1.0/workspaces/cgf/cgf/datastore.xml

   .. code-block:: shell

    cd <root of geoserver install>
    export GEOSERVER_DATA_DIR=<path of geoserver repository>/build/cite/wfs10/citewfs-1.0
    ./bin/startup.sh

#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL`` http://<ip-of-the-geoserver>:8080/geoserver/wfs?request=getcapabilities&service=wfs&version=1.0.0

   #. ``Enable tests with multiple namespaces`` tests included

      .. image:: ./image/tewfs-1_0.png

Run WFS 1.1 tests
-----------------

.. important::

   Running WFS 1.1 tests requires PostgreSQL with PostGIS extension installed in the system.

Requirements:
~~~~~~~~~~~~~
- GeoServer
- teamengine
- Posgresql
- PostGIS

#. Prepare the environment:

   - login to postgresql and create a user named "cite".

   .. code:: sql

     createuser cite;

   - Create a database named "cite", owned by the "cite" user:

   .. code:: sql

     createdb cite own by cite;

   - enter to the database and enable the postgis extension:

   .. code:: sql

    create extension postgis;

   - Change directory to the citewfs-1.1 data directory and execute the script dataset-sf0-postgis2.sql:

   .. code-block:: shell

    cd <path of geoserver repository>
    psql -U cite cite < build/cite/wfs11/citewfs-1.1/dataset-sf0-postgis2.sql

   - Start GeoServer with the citewfs-1.1 data directory. Example:

   .. important::

     If the postgresql server is not in the same host of the geoserver, you have to change the `<entry key="host">localhost</entry>` in the `datastore.xml` file, located inside each workspace directory. ex.

     .. note::

       <path of geoserver repository>/build/cite/wfs11/citewfs-1.1/workspaces/cgf/cgf/datastore.xml

   .. code-block:: shell

    cd <path of geoserver install>
    export GEOSERVER_DATA_DIR=<path of geoserver repository>/build/cite/wfs11/citewfs-1.1
    ./bin/startup.sh


#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL`` http://<ip-of-the-geoserver>:8080/geoserver/wfs?service=wfs&request=getcapabilities&version=1.1.0

   #. ``Supported Conformance Classes``:

      * Ensure ``WFS-Transaction`` is *checked*
      * Ensure ``WFS-Locking`` is *checked*
      * Ensure ``WFS-Xlink`` is *unchecked*

      .. image:: ./image/tewfs-1_1a.png

   #. ``GML Simple Features``: ``SF-0``

   .. image:: ./image/tewfs-1_1b.png

Run WMS 1.1 tests
-----------------

#. Prepare the environment:

  - Start GeoServer with the citewms-1.1 data directory. Example:

   .. code-block:: shell

    cd <root of geoserver install>
    export GEOSERVER_DATA_DIR=<path of geoserver repository>/build/cite/wms11/citewms-1.1
    ./bin/startup.sh

#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL``

          http://<ip-of-the-geoserver>:8080/geoserver/wms?service=wms&request=getcapabilities&version=1.1.1

   #. ``UpdateSequence Values``:

      * Ensure ``Automatic`` is selected
      * "2" for ``value that is lexically higher``
      * "0" for ``value that is lexically lower``

   #. ``Certification Profile`` : ``QUERYABLE``

   #. ``Optional Tests``:

      * Ensure ``Recommendation Support`` is *checked*
      * Ensure ``GML FeatureInfo`` is *checked*
      * Ensure ``Fees and Access Constraints`` is *checked*
      * For ``BoundingBox Constraints`` ensure ``Either`` is selected

   #. Click ``OK``

   .. image:: ./image/tewms-1_1a.png

   .. image:: ./image/tewms-1_1b.png

Run WCS 1.0 tests
-----------------

#. Prepare the environment:

  - Start GeoServer with the citewcs-1.0 data directory. Example:

   .. code-block:: shell

    cd <root of geoserver install>
    export GEOSERVER_DATA_DIR=<path of geoserver repository>/build/cite/wcs10/citewcs-1.0
    ./bin/startup.sh

#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL``:

          http://<ip-of-the-geoserver>:8080/geoserver/wcs?service=wcs&request=getcapabilities&version=1.0.0

   #. ``MIME Header Setup``: "image/tiff"

   #. ``Update Sequence Values``:

      * "2" for ``value that is lexically higher``
      * "0" for ``value that is lexically lower``

   #. ``Grid Resolutions``:

      * "0.1" for ``RESX``
      * "0.1" for ``RESY``

   #. ``Options``:

      * Ensure ``Verify that the server supports XML encoding`` is *checked*
      * Ensure ``Verify that the server supports range set axis`` is *checked*

   #. ``Schemas``:

      * Ensure that ``The server implements the original schemas from the WCS 1.0.0 specification (OGC 03-065`` is selected

   #. Click ``OK``

   .. image:: ./image/tewcs-1_0a.png

   .. image:: ./image/tewcs-1_0b.png

   .. image:: ./image/tewcs-1_0c.png


Run WCS 1.1 tests
-----------------

#. Prepare the environment:

  - Start GeoServer with the citewcs-1.1 data directory. Example:

   .. code-block:: shell

    cd <root of geoserver install>
    export GEOSERVER_DATA_DIR=<root of geoserver sources>/build/cite/wcs11/citewcs-1.1
    ./bin/startup.sh


#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL``:

         http://<ip-of-the-geoserver>:8080/geoserver/wcs

   Click ``Next``

   .. image:: ./image/tewcs-1_1a.png


Run WMS 1.3 tests
-----------------

#. Prepare the environment:

  - Start GeoServer with the citewcs-1.3 data directory. Example:

   .. code-block:: shell

    cd <root of geoserver install>
    export GEOSERVER_DATA_DIR=<root of geoserver sources>/build/cite/wms13/citewms-1.3
    ./bin/startup.sh


#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL``:

         http://<ip-of-the-geoserver>:8080/geoserver/wms?service=wms&request=getcapabilities&version=1.3.0

   #. ``UpdateSequence Values``:

      * ``Automatic`` *checked*

   #. ``Options``:

      * Ensure ``BASIC`` is *checked*
      * Ensure ``QUERYABLE`` is *checked*

   Click ``OK``

   .. image:: ./image/tewms-1_3.png



.. _commandline:

.. _teamengine:
