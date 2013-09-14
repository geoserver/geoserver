.. _community_jdbcconfig_installing:

Installing JDBCConfig
=====================

To install the JDBCConfig module:

#. `Download <http://geoserver.org/display/GEOS/Download>`_ the module. The file name is called :file:`geoserver-*-jdbcconfig-plugin.zip`, where ``*`` is the version/snapshot name.

#. Extract this file and place the JARs in ``WEB-INF/lib``.

#. Perform any configuration required by your servlet container, and then restart. Warning: you are now starting JDBCConfig with a default configuration, which uses a H2 database to store the catalog. If you want to use another type of database, please refer to the :ref:`community_csw_config` and configure this before you start the server.

#. Verify that the module was installed correctly: after you have started GeoServer for the first time with the jdbcconfig module, a directory jdbcconfig should be present in the :ref:`data_directory`. During this first start-up, your old catalog will have been automatically copied into the relational database. 
