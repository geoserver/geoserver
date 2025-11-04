OGC Testbed Experiments
=======================

The following modules are part of the OGC Testbed experiments.

Images

An extra API, images, based on engineering reports of `Testbed 15 <https://docs.ogc.org/per/19-018.html>`__,
allowing to update a image mosaic.

To install this module:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download ``ogcapi-images`` zip archive.
   
   * |version| example: :nightly_community:`ogcapi-images`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-loader-plugin.zip above).

#. Restart GeoServer.

#. The services are listed at http://localhost:8080/geoserver

DGGS
----

The DGGS functionality exposes data structured as DGGS, based on either H2 or rHealPix,
based on engineering reports of `Testbed 18 <https://docs.ogc.org/per/20-039r2.html>`__.

To make full usage of the DGGS functionality, a ClickHouse installation is also needed.

To install these modules:

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Development** tab,
   and locate the nightly release that corresponds to the GeoServer you are running.
   
   Follow the **Community Modules** link and download ``ogcapi-dggs`` zip archive.
   
   * |version| example: :nightly_community:`ogcapi-dggs`
   
   The website lists active nightly builds to provide feedback to developers,
   you may also `browse <https://build.geoserver.org/geoserver/>`__ for earlier branches.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory in GeoServer.

   .. warning:: Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-|version|-ogcapi-dggs-plugin.zip above).

#. Restart GeoServer.

   On restart the services are listed at http://localhost:8080/geoserver

The DGGS datastore can read data from a ClickHouse database. In case you want to use JNDI along
with Tomcat, here is a sample data source declaration for the ``etc/context.xml`` file:

.. code-block:: xml
    
   <Resource name="jdbc/clickhouse" auth="Container" type="javax.sql.DataSource"
               maxTotal="100" maxIdle="30" maxWaitMillis="10000"
               username="myUser" password="myPassword" driverClassName="ru.yandex.clickhouse.ClickHouseDriver"
               url="jdbc:clickhouse://localhost:8123/theDatabase?http_connection_provider=HTTP_CLIENT"/>