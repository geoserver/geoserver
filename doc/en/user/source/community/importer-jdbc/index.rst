.. _community_importer_jdbc:

Importer JDBC storage
=====================

This plugin allows sharing the state of imports in a relational database supported by GeoTools.
Compared to the default in-memory storage, this helps in two ways:

* The state of the imports is persisted and survives restarts
* If an external database, such as PostgreSQL/PostGIS, is used, then it's possible to run multiple 
  imports on different GeoServer nodes in load balancing and get a consolidated view of their state

Installation
------------

The module zip just need to be unpacked in the GeoServer ``WEB-INF\lib``. 
On startup the module will create by default a H2 database in a "importer" folder
inside the data directory, as well as a configuration file at ``${GEOSERVER_DATA_DIR}/jdbc-import-store.properties``.

The property file can be modified to point to an external database, for example, the following
contents are suitable for a PostGIS database (PostGIS extensions mandatory, even if not used):

.. code-block:: scss

    dbtype=postgis
    user=myUserName
    passwd=myPassword
    database=databaseName
    port=5432
    host=localhost
    schema=public

On connection the code will create the tables as well as suitable indexes on the one table used to
track the imports. In case the user above is not allowed to create tables, the following SQL
statement can be used (adapt to the specific database):

.. code-block:: sql
    
    CREATE TABLE public.import_context
    (
      fid serial,
      context text,
      created timestamp,
      updated timestamp,
      "user" character varying,
      state character varying,
      CONSTRAINT import_context_pkey PRIMARY KEY (fid)
    );
    CREATE INDEX import_context_state ON import_context(state);
    CREATE INDEX import_context_user ON import_context("user");

.. note:: The store has been tested with H2 and Postgresql with PostGIS extensions, it may work
  with other relational databases too assuming that they have a corresponding GeoTools data store
  plugin installed and the database is not changing the name of the columns (Oracle will most 
  likely not work).

.. note:: With some light extra development and testing the code could be extended to save the status
  of imports in any GeoTools supported store, e.g., SOLR, MongoDB.
