.. _community_jdbcstore_installing:

Installing JDBCStore
====================

To install the JDBCStore module:

#. `Download <http://geoserver.org/download>`_ the module. The file name is called :file:`geoserver-*-jdbcstore-plugin.zip`, where ``*`` is the version/snapshot name. The JDBCStore plug-in automatically includes the :ref:`community_jdbcconfig` plugin as well which will generally be run at the same time.

#. Extract this file and place the JARs in ``WEB-INF/lib``. 

#. Perform any configuration required by your servlet container, and then restart. On startup, JDBCStore will create a configuration directory ``jdbcstore`` and JDBCConfig will create a configuration directory ``jdbcconfig`` in the :ref:`datadir` .

#. Verify that the configuration directories were created to be sure installation worked then turn off GeoServer.

#. If you want to use :ref:`community_jdbcconfig` as well, configure it first, being sure to set ``enabled``, ``initdb``, and ``import`` to ``true``, and to provide the connection information for an empty database. Start GeoServer to initialize the JDBCConfig database, import the old catalog into it, and take over from the old catalog. Subsequent start ups will skip the initialize and import steps unless you re-enable them in ``jdbcconfig.properties``.

#. Now configure JDBCStore in a similar fashion (:ref:`community_jdbcstore_config`), being sure to set ``enabled``, ``initdb``, and ``import`` to ``true``, and to provide the connection information for an empty database. Start GeoServer again.  This time JDBCStore will connect to the specified database, initialize it, import the old :ref:`datadir` into it, and take over from the old :ref:`datadir`. Subsequent start ups will skip the initialize and import steps unless you re-enable them in ``jdbcstore.properties``.

