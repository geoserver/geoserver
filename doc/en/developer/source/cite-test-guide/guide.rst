CITE automation tests with docker
=================================


How to automate the CITE Tests with
`docker <https://www.docker.com>`_.

Requirements:
-------------

- Running the tests requires a linux system with `docker <https://www.docker.com>`_, `docker-compose <https://docs.docker.com/compose/install>`_, and git installed on it.

.. note::

The CITE tools are available in the build/cite folder of the `GeoServer repository <https://github.com/geoserver/geoserver/tree/master/build/cite>`_:

Steps:
------

**Set-up the environment.**
~~~~~~~~~~~~~~~~~~~~~~~~~~~

   #.  Clone the repository.

       .. code:: shell

          git clone https://github.com/randomorder/geoserver.git --branch GSIP-176

   #.  go the cite directory.

       .. code:: shell

          cd geoserver/build/cite

   #.  inside will find a structure like below with a list of directories with the name of the suites to run.

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
          |-- logs
          |-- docker-compose.yml
          |-- postgres
          |-- README.md
          `-- Makefile

**Running the suite tests.**
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

   There is 2 way to run the suites, one is running with make that will
   automate all the commands, and the second one is running the test through WebUI:

   1. Running the through Makefile:

      -  run ``make`` in the console, will give you the list of commands
         to run.

         .. code:: shell

            make

      -  the output will like this:

         .. code:: makefile

            clean: $(suite)         Will Clean the Environment of previous runs.
            build: $(suite)         Will Build the GeoServer Docker Image for the Environment.
            test: $(suite)      Will running the Suite test with teamengine.
      - Choose which test to run by setting the Suite environment variable:

        .. code:: SHELL

           suite=wcs10

        .. note::

           Valid values for the Suite parameter are
             * wcs10
             * wcs11
             * wfs10
             * wfs11
             * wms11
             * wms13

      - Choose which GeoServer war to test by setting the ``war_url`` environment variable inside the ``Makefile``, ex:

        .. code:: C

          war_url = "https://build.geoserver.org/geoserver/master/geoserver-master-latest-war.zip"

      -  To clean the local environment.

         .. code:: shell

            make clean suite=<suite-name>

      -  To build the geoserver docker image locally.

         .. code:: shell

            make build suite=<suite-name>

      -  To run the suite test.

         .. code:: shell

            make test suite=<suite-name>

      -  And the last, but no less important run the full automate
         workflow.

         .. note::

            The first Docker build may take a long time.

         .. code:: shell

            make clean build test suite=<suite-name>

   2. Running the test in the WebUI.

      - To run the test in the WebUI, you should change the ``command`` parameter in the ``docker-compose.override.yml``.
      - To do so, you get in to any folder of the suite that you want to run.

        .. code:: SHELL

           cd wcs10

      - change ``command: /run-test.sh wcs10`` to ``command: /run-test.sh interactive``
      - map the port of the teamengine to the port of your preference in the host. ex: change ``8080`` to ``8888:8080``

        .. code:: YAML

           ports:
             - 8888:8080

      - then run ``make test suite=wcs10``
      - when the command finish to build teamengine image and run the container, you can access to the WebUI through the browser at: ``http://localhost:8888``
      - after finish the test run in the terminal ``make clean suite=<suite-name>``

How to run TEAM Engine standalone
---------------------------------
- To run a standalone version of TEAM Engine, start it with the following command:

  .. code:: SHELL

     docker run -d --name standalone_teamengine -p 8080:8080 geosolutionsit/teamengine:latest

- TEAM Engine will be accessible on http://localhost:8080/teamengine/

- If you want to change the port, for example to have it on port "9090", change the command as follows:

  .. code:: SHELL

     docker run -d --name standalone_teamengine -p 9090:8080 geosolutionsit/teamengine:latest

- To stop TEAM Engine:

  .. code:: SHELL

     docker stop standalone_teamengine

