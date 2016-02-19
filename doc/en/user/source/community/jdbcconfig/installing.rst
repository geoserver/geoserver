.. _community_jdbcconfig_installing:

Installing JDBCConfig
=====================

To install the JDBCConfig module:

#. `Download <http://geoserver.org/download>`_ the module. The file name is called :file:`geoserver-*-jdbcconfig-plugin.zip`, where ``*`` is the version/snapshot name.

#. Extract this file and place the JARs in ``WEB-INF/lib``.

#. Perform any configuration required by your servlet container, and then restart. On startup, JDBCConfig will create a configuration directory ``jdbcconfig`` in the :ref:`datadir`.

#. Verify that the configuration directory was created to be sure installation worked then turn off GeoServer.

#. Configure JDBCConfig (:ref:`community_jdbcconfig_config`), being sure to set ``enabled``, ``initdb``, and ``import`` to ``true``, and to provide the connection information for an empty database.

#. Start GeoServer again.  This time JDBCConfig will connect to the specified database, initialize it, import the old catalog into it, and take over from the old catalog. Subsequent start ups will skip the initialize and import steps unless you re-enable them in ``jdbcconfig.properties``.

#. Log in as admin and a message should appear on the welcome page:

.. image:: h2message.png
