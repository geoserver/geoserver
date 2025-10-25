.. _community_jdbcstore_installing:

Installing JDBCStore
====================

To install the JDBCStore module:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download `jdbcstore` zip archive.
   
   * |version| example: :nightly_community:`jdbcstore`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

   The JDBCStore plug-in includes the :ref:`community_jdbcconfig` plugin as well which will generally be run at the same time.

#. Perform any configuration required by your servlet container, and then restart.
   
   On startup, JDBCStore will create a configuration directory :file:`jdbcstore`
   and JDBCConfig will create a configuration directory :file:`jdbcconfig` in the :ref:`datadir` .

#. Verify that the configuration directories were created to be sure installation worked then turn off GeoServer.

#. If you want to use :ref:`community_jdbcconfig` as well, configure it first, being sure to set ``enabled``, ``initdb``, and ``import`` to ``true``, and to provide the connection information for an empty database. Start GeoServer to initialize the JDBCConfig database, import the old catalog into it, and take over from the old catalog. Subsequent start ups will skip the initialize and import steps unless you re-enable them in ``jdbcconfig.properties``.

#. Now configure JDBCStore in a similar fashion (:ref:`community_jdbcstore_config`), being sure to set ``enabled``, ``initdb``, and ``import`` to ``true``, and to provide the connection information for an empty database. Start GeoServer again.  This time JDBCStore will connect to the specified database, initialize it, import the old :ref:`datadir` into it, and take over from the old :ref:`datadir`. Subsequent start ups will skip the initialize and import steps unless you re-enable them in ``jdbcstore.properties``.

