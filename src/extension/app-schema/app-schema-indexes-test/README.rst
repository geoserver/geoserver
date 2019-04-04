Simple Store Online Tests
^^^^^^^^^^^^^^^^^^^^^^^^^

Apache Solr data store online tests will run if a fixture file exists and contains
the URL of a valid Apache Solr instance core.

The fixture file is expect to exist in the user home directory. In Linux, the
expected path is this one:

.. code-block:: properties

        ~/.geoserver/appschema-indexes.properties

The fixture file should look like this:

.. code-block:: properties

        solr_url=http://localhost:8983/solr/test_core
        solr_core=test_core
        pg_host=localhost
        pg_port=5432
        pg_database=database
        pg_user=myuser
        pg_password=1234
        

The fixture file only needs to contain the URL of a valid Apache Solr instance core.
No tests were made with SolarCloud. The tests will take care of creating
the necessary schemas and load the necessary data.

Unfortunately is not possible to create using Solar API a new core in a
reliable way. So before running this plugin integration tests a core needs to be
manually created.

It is worth noticing that support for advanced geometries, like polygons, is necessary. Which
means that JTS support should be correctly configured in the Apache Solr instance.

A new core can be created using Apache Solr web console ``http://localhost:8983/solr/#/~cores``.
Although, this method requires the manual setup of the core directory in the server.

The easiest option is to use the ``solr`` server binary to create the desired core:

.. code-block:: bash

        > bin/solr create -c test_core

        Copying configuration to new core instance directory:
        /servers/solr-6.6.1/server/solr/test_core

        Creating new core 'test_core' using command:
        http://localhost:8983/solr/admin/cores?action=CREATE&name=test_core&instanceDir=test_core

        {
          "responseHeader":{
            "status":0,
            "QTime":782},
          "core":"test_core"}

Note that the integration tests will be able to reuse the same core for multiple runs as long
as the core managed schema is not manually edited.

If any error happens during the integration tests execution it is strongly recommended to use
a clean core for new runs. There is no way to reset a core, the existing core needs to be manually
removed and\or a new one created.

The easiest way to remove a core is using the ``solr`` binary to remove the desired core:

.. code-block:: bash

        > ./bin/solr delete -c test_core

        Deleting core 'test_core' using command:
        http://localhost:8983/solr/admin/cores?action=UNLOAD&core=test_core&deleteIndex=true&deleteDataDir=true&deleteInstanceDir=true

        {"responseHeader":{
            "status":0,
            "QTime":9}}

It is highly recommended to read Apache Solr documentation related with core managements.

On Windows:
	.\bin\solr start
	.\bin\solr delete -c stations
	.\bin\solr create -c stations
	


