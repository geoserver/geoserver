.. _cite_test_guide:

Cite Test Guide
===============

A step by step guide to the GeoServer Compliance Interoperability Test Engine (CITE).

.. contents::

~~~~~~~~~~~~~


Check out OGC CITE suite tests
------------------------------

.. note:: The CITE suite tests are available at `Open Geospatial Consortium`_.
.. _Open Geospatial Consortium: https://github.com/opengeospatial

Requirements:

- `GeoServer instance <https://github.com/geoserver/geoserver>`_.

- `Teamengine Web Application <https://github.com/geosolutions-it/teamengine-docker>`_, with a set of CITE suite tests.

- ``make``


CITE automation tests with docker
---------------------------------


How to run the CITE Test suites with
`docker <https://www.docker.com>`_.

Requirements:

- Running the tests requires a Linux system with `docker <https://www.docker.com>`_, `docker-compose <https://docs.docker.com/compose/install>`_, and Git installed on it.

.. note::

   The CITE tools are available in the build/cite folder of the `GeoServer Git repository <https://github.com/geoserver/geoserver/tree/master/build/cite>`_:

Set-up the environment
^^^^^^^^^^^^^^^^^^^^^^

#.  Clone the repository.

    .. code:: shell

       git clone https://github.com/geoserver/geoserver.git

#.  Go to the cite directory.

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
       |-- ogcapi-features10
       |-- interactive
       |-- logs
       |-- docker-compose.yml
       |-- postgres
       |-- README.md
       `-- Makefile

Running the suite tests
^^^^^^^^^^^^^^^^^^^^^^^

There are 2 ways to run the suites. One is running with ``make`` that will
automate all the commands, and the second one is running the test through WebUI:

1. Running it through ``Makefile``:

   -  run ``make`` in the console, it will give you the list of commands
      to run.

      .. code:: shell

         make

   -  the output will look like this:

      .. code:: makefile

         Usage:

         # Main targets in suggested order:

         war:	 					Build the geoserver.war file to use for testing and place it in ./geoserver/geoserver.war
         build: 	suite=<suite>				Build the GeoServer Docker Image for the Environment.
         test: 	suite=<suite>				Run the Test Suite with teamengine and GeoServer on docker compose.
         clean:	 					Clean the Environment of previous runs.

         # Additional helper targets:

         test-localhost:  suite=<suite>			Run the Test Suite against a local host GeoServer instance (http://172.17.0.1:8080)
         test-external:  suite=<suite> iut=<landing URL>	Run the Test Suite against a GeoServer instance at a provided URL
         version:  suite=<suite>				Print the version of the GeoServer on the current docker.
         ogcapi-features10-localhost: 			Shortcut for make test-localhost suite=ogcapi-features10
         start:  suite=<suite> [services=<s1 s2..>]	Start the docker composition for suite. Optionally limit which services.
         stop: 						Shuts down the docker composition. Deos not remove logs/
         print-services:  suite=<suite>			Print the service names and docker images used for a given suite
         webUI: 						Start teamengine in interactive mode for the OWS services (excludes ogcapi services).


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
          * ogcapi-features10

   - Build the ``geoserver.war`` file to test against :

     .. code:: C

       make war

2. Build the GeoServer Docker image set up to run a specific test suite

   -  To clean the local environment.

      .. code:: shell

         make clean

   -  To build the GeoServer Docker image locally.

      .. code:: shell

         make build suite=<suite-name>

   - Alternative, specify a ``war_url`` variable to fetch the ``geoserver.war`` from an URL:

      .. code::

        make build suite=<suite-name> war_url=<url-or-the-GeoServer-war-file-desired>

   The ``war_url`` can point to a ``.war`` or ``.zip`` file containing the ``.war`` like in ``https://build.geoserver.org/geoserver/main/geoserver-main-latest-war.zip``

   -  To run the suite test.

      .. code:: shell

         make test suite=<suite-name>

   -  To run the full automate workflow.


      .. code:: shell

         make clean build test suite=<suite-name>


Run CITE Test Suites on a local PC
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. note::

   I assume that you have a standalone GeoServer running.

.. important::

   Details to consider when you are running the tests:

   - The default username/password for the teamengine webUI are **teamengine/teamengine**.

   - the default URL for the teamengine webUI is http://localhost:8888/teamengine/

   - The output of the old suite tests might not appear in the Result page. So you should click on the link below **detailed old test report**, to get the full report. Ex.

   .. image:: ./image/old-report.png

   .. image:: ./image/full-report.png

   - Since you are running teamengine inside a container, the localhost in the URL of GeoServer for the tests can't be used, for that, get the IP address of the host where the GeoServer is running. You will use it later.

   - after you log in to teamengine webUI you have to create a session.

   .. image:: ./image/seccion.png

   - to run the tests you have to choose which one you want, and then click on **Start a new test session**. This is an example:

   .. image:: ./image/tewfs-1_0a.png


Requirements:

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

        clean: $(suite)		 This will clean the Environment of previous runs.
        build: $(suite)		 This will build the GeoServer Docker Image for the Environment.
        test: $(suite)		 This will run the Suite test with teamengine.
        webUI: $(suite)		 This will run the Suite test with teamengine.


Run WFS 1.0 tests
^^^^^^^^^^^^^^^^^

.. important::

   Running WFS 1.0 tests require PostgreSQL with PostGIS extension installed in the system.

Requirements:

- `GeoServer running`
- teamengine
- PostgreSQL
- PostGIS

#. Prepare the environment:

   - login to PostgreSQL and create a user named "cite".

   .. code:: sql

     createuser cite;

   - Create a database named "cite", owned by the "cite" user:

   .. code:: sql

     createdb cite own by cite;

   - enter the database and enable the postgis extension:

   .. code:: sql

    create extension postgis;

   - Change directory to the citewfs-1.0 data directory and execute the script cite_data_postgis2.sql:

   .. code-block:: shell

    cd <path of GeoServer repository>
    psql -U cite cite < build/cite/wfs10/citewfs-1.0/cite_data_postgis2.sql

   - Start GeoServer with the citewfs-1.0 data directory. Example:

   .. important::

     If the PostgreSQL server is not on the same host as the GeoServer, you have to change the `<entry key="host">localhost</entry>` in the `datastore.xml` file, located inside each workspace directory. ex.

     .. note::

       <path of GeoServer repository>/build/cite/wfs10/citewfs-1.0/workspaces/cgf/cgf/datastore.xml

   .. code-block:: shell

    cd <root of GeoServer install>
    export GEOSERVER_DATA_DIR=<path of GeoServer repository>/build/cite/wfs10/citewfs-1.0
    ./bin/startup.sh

#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL`` http://<ip-of-the-GeoServer>:8080/geoserver/wfs?request=getcapabilities&service=wfs&version=1.0.0

   #. ``Enable tests with multiple namespaces`` tests included

      .. image:: ./image/tewfs-1_0.png

Run WFS 1.1 tests
^^^^^^^^^^^^^^^^^

.. important::

   Running WFS 1.1 tests requires PostgreSQL with PostGIS extension installed in the system.

Requirements:
- GeoServer
- teamengine
- PostgreSQL
- PostGIS

#. Prepare the environment:

   - login to PostgreSQL and create a user named "cite".

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

    cd <path of GeoServer repository>
    psql -U cite cite < build/cite/wfs11/citewfs-1.1/dataset-sf0-postgis2.sql

   - Start GeoServer with the citewfs-1.1 data directory. Example:

   .. important::

     If the PostgreSQL server is not on the same host as the GeoServer, you have to change the `<entry key="host">localhost</entry>` in the `datastore.xml` file, located inside each workspace directory. ex.

     .. note::

       <path of GeoServer repository>/build/cite/wfs11/citewfs-1.1/workspaces/cgf/cgf/datastore.xml

   .. code-block:: shell

    cd <path of GeoServer install>
    export GEOSERVER_DATA_DIR=<path of GeoServer repository>/build/cite/wfs11/citewfs-1.1
    ./bin/startup.sh


#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL`` http://<ip-of-the-GeoServer>:8080/geoserver/wfs?service=wfs&request=getcapabilities&version=1.1.0

   #. ``Supported Conformance Classes``:

      * Ensure ``WFS-Transaction`` is *checked*
      * Ensure ``WFS-Locking`` is *checked*
      * Ensure ``WFS-Xlink`` is *unchecked*

      .. image:: ./image/tewfs-1_1a.png

   #. ``GML Simple Features``: ``SF-0``

   .. image:: ./image/tewfs-1_1b.png

Run WMS 1.1 tests
^^^^^^^^^^^^^^^^^

#. Prepare the environment:

  - Start GeoServer with the citewms-1.1 data directory. Example:

   .. code-block:: shell

    cd <root of GeoServer install>
    export GEOSERVER_DATA_DIR=<path of GeoServer repository>/build/cite/wms11/citewms-1.1
    ./bin/startup.sh

#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL``

          http://<ip-of-the-GeoServer>:8080/geoserver/wms?service=wms&request=getcapabilities&version=1.1.1

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
^^^^^^^^^^^^^^^^^

#. Prepare the environment:

  - Start GeoServer with the citewcs-1.0 data directory. Example:

   .. code-block:: shell

    cd <root of GeoServer install>
    export GEOSERVER_DATA_DIR=<path of GeoServer repository>/build/cite/wcs10/citewcs-1.0
    ./bin/startup.sh

#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL``:

          http://<ip-of-the-GeoServer>:8080/geoserver/wcs?service=wcs&request=getcapabilities&version=1.0.0

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
^^^^^^^^^^^^^^^^^

#. Prepare the environment:

  - Start GeoServer with the citewcs-1.1 data directory. Example:

   .. code-block:: shell

    cd <root of GeoServer install>
    export GEOSERVER_DATA_DIR=<path of GeoServer repository>/build/cite/wcs11/citewcs-1.1
    ./bin/startup.sh


#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL``:

         http://<ip-of-the-GeoServer>:8080/geoserver/wcs

   Click ``Next``

   .. image:: ./image/tewcs-1_1a.png


Run WMS 1.3 tests
^^^^^^^^^^^^^^^^^

#. Prepare the environment:

  - Start GeoServer with the citewcs-1.3 data directory. Example:

   .. code-block:: shell

    cd <root of GeoServer install>
    export GEOSERVER_DATA_DIR=<path of GeoServer repository>/build/cite/wms13/citewms-1.3
    ./bin/startup.sh


#. Start the test:

   .. code:: shell

     make webUI

#. Go to the browser and open the teamengine `webUI <http://localhost:8888/teamengine>`_.

   - click on the **Sign in** button and enter the user and password.

   - after creating the session, and choosing the test, enter the following parameters:

   #. ``Capabilities URL``:

         http://<ip-of-the-GeoServer>:8080/geoserver/wms?service=wms&request=getcapabilities&version=1.3.0

   #. ``UpdateSequence Values``:

      * ``Automatic`` *checked*

   #. ``Options``:

      * Ensure ``BASIC`` is *checked*
      * Ensure ``QUERYABLE`` is *checked*

   Click ``OK``

   .. image:: ./image/tewms-1_3.png


Run OGC Features 1.0 tests
^^^^^^^^^^^^^^^^^^^^^^^^^^

Newer test suites like the ``ogcapi-features10`` one, are executed by calling teamengine's REST API,
with a teamengine Docker image `provided by OGC <https://hub.docker.com/r/ogccite/ets-ogcapi-features10>`_ (see `Using the REST API <https://opengeospatial.github.io/teamengine/users.html>`_ section
on the teamengine's user guide).

As a result of the test run, a ``logs/testng-results.xml`` file will be generated, and a human readable summary of test
failures, if any, will be printed to the console.

Run with the locally built .war
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Make sure you've prepared the ``geoserver.war`` as instructed above with ``make war``.

   .. code-block:: shell

    make clean build test suite=ogcapi-features10

If there are test errors, a human readable summary will be printed to the console, similar to this:

   .. code-block:: shell

      test-method: verifyCollectionsPathCollectionCrsPropertyContainsDefaultCrs
      description: Implements A.1 Discovery, Abstract Test 2 (Requirement /req/crs/fc-md-crs-list B), crs property contains default crs in the collection objects in the path /collections
      depends-on-groups: crs-conformance
      status: FAIL
      exception: Collection with id 'sf:restricted' at collections path /collections does not specify one of the default CRS 'http://www.opengis.net/def/crs/OGC/1.3/CRS84' or 'http://www.opengis.net/def/crs/OGC/0/CRS84h' but provides at least one spatial feature collections
      Request URI:

      test-method: verifyCollectionsPathCollectionCrsPropertyContainsDefaultCrs
      description: Implements A.1 Discovery, Abstract Test 2 (Requirement /req/crs/fc-md-crs-list B), crs property contains default crs in the collection objects in the path /collections
      depends-on-groups: crs-conformance
      status: FAIL
      exception: Collection with id 'sf:roads' at collections path /collections does not specify one of the default CRS 'http://www.opengis.net/def/crs/OGC/1.3/CRS84' or 'http://www.opengis.net/def/crs/OGC/0/CRS84h' but provides at least one spatial feature collections
      Request URI:

      Passed: 2153
      Failed: 9
      Skipped: 96
      make[2]: *** [validate-testng-results] Error 1
      make[1]: *** [test-rest] Error 2
      make: *** [test] Error 2


Either way, both the ``teamengine`` and ``geoserver`` containers will keep on running.

Run ``make clean`` to shut them down and clean up the ``logs/`` directory.

Test a GeoServer instance external to the docker composition
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Since teamengine runs as a Docker container, in order to reach out to a GeoServer instance running on the host,
it needs a Landing Page URL that points to the host network. In docker there's a special IP address for that purpose,
`172.17.0.1`, as long as the container is running on the default docker bridge network. Check out the docker [docs](Networking with standalone containers) for more info.

.. attention::

   In the following examples, some ``make`` targets receive an ``iut`` parameter with the URL of the OGC Features API landing page to test,
   external to the ``teamengine``'s container network. By default, for **Linux** systems, use the **172.17.0.1** IP address.
   However, if you're running the tests on **MacOS**, replace it with the **host.docker.internal** hostname instead.
   This difference exists because on Linux, Docker creates a bridge network where the host is accessible via ``172.17.0.1``. On MacOS, Docker Desktop for Mac
   runs containers within a virtualization layer, which changes the networking model. As a result, ``host.docker.internal`` is used to enable containers
   to access the host.


For the case of the ``ogcapi-features10``, you can simply run 

   .. code-block:: shell

    make ogcapi-features10-localhost

And it'll print out

   .. code-block:: shell

    Running the ogcapi-features10 test suite with the teamengine REST API against http://172.17.0.1:8080/geoserver/ogc/features/v1

The ``ogcapi-features10-localhost`` target is a special case of ``test-external``, which assumes the most common
case of GeoServer running on ``localhost:8080``.

During development or troubleshooting, you might want to either use a different GeoServer port, or
test only a specific workspace or feature type. For that you can use a custom ``iut`` (Instance Under Test)
URL for the ``test-external`` make target. For example, to hit a GeoServer instance running on the host
at port ``9090``, and address only the ``sf:archsites`` layer, you can use a ``iut`` URL combining the 
``172.17.0.1`` IP address and GeoServer's ``/sf/archsites`` virtual service:

   .. code-block:: shell

    make test-external suite=ogcapi-features10 iut="http://172.17.0.1:9090/geoserver/sf/archsites/ogc/features/v1"


And it'll print out

   .. code-block:: shell

    Running the ogcapi-features10 test suite with the teamengine REST API against http://172.17.0.1:9090/geoserver/sf/archsites/ogc/features/v1

Finally, run

   .. code-block:: shell

    make clean

to stop the docker composition and clean up the ``logs/`` directory, or

   .. code-block:: shell

    make stop

to just shut down the docker composition wihtout cleaning up the ``logs/`` directory.

.. _commandline:

.. _teamengine:

