.. _community_jdbcstore_config:

JDBCStore configuration
========================

The JDBCStore module is configured in the file ``jdbcstore/jdbcstore.properties`` inside the :ref:`datadir`.
The following properties may be set:

- ``enabled``: Use JDBCStore.  Turn off to use the data directory for all configuration instead.

- ``initdb``: Initialize an empty database if this is set on true. 

- ``import`` : The import configuration option tells GeoServer whether to import the current :ref:`datadir` from the file system to the database or not. If set to true, it will be imported and the config option will be set the value 'false' for the next start up to avoid trying to re-import the catalog  configuration.

- ``initScript``: Path to initialisation script .sql file. Only used if ``initdb`` is true.

- ``ignoreDirs``: specify all subdirectories of the :ref:`datadir` that should be ignored by the JDBCStore, in a comma-separate list. These subdirectories will not be imported and while JDBCStore is running, all access to these subdirectories and their contents will be redirected to the default file system store. This is usually done with the ``data`` directory (which holds data rather than metadata such as images and shapefiles), temporary directories (which are not used for permanent storage) and the catalog directories (when using JDBCConfig, these are unused anyway and they need not be copied into the JDBCStore).

- ``deleteDestinationOnRename``: allow automatic overwriting of existing destinations on move and rename operations (linux-style versus windows-style - the default store is platform dependant).

JNDI
~~~~

Get the database connection from the application server via JNDI lookup. 

- ``jndiName``: The JNDI name for the data source. Only set this if you want to use JNDI, the JDBC configuration properties may still be set for in case the JNDI Lookup fails. 

Direct JDBC Connection
~~~~~~~~~~~~~~~~~~~~~~

Provide the connection parameters directly in the configuration file. This includes the password in the clear which is a potential security risk.  To avoid this use JNDI instead.

- ``jdbcUrl``: JDBC direct connection parameters. 

- ``username``: JDBC connection username.

- ``password``: JDBC connection password.

- ``pool.minIdle``: minimum connections in pool  

- ``pool.maxActive``:  maximum connections in pool

- ``pool.poolPreparedStatements``: whether to pool prepared statements

- ``pool.maxOpenPreparedStatements``: size of prepared statement cache, only used if ``pool.poolPreparedStatements`` is true

- ``pool.testOnBorrow``: whether to validate connections when obtaining from the pool

- ``pool.validationQuery``: validation query for connections from pool, must be set when ``pool.testOnBorrow`` is true

