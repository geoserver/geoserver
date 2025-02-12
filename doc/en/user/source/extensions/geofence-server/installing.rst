.. _geofence_server_install:

Installing the GeoServer GeoFence Server extension
==================================================


Install the plugin
------------------

 #. Visit the :website:`website download <download>` page, locate your release, and download: :download_extension:`geofence-server`
   
    The download link will be in the :guilabel:`Extensions` section under :guilabel:`Other`.
   
    Make sure to match the plugin version (e.g. |release| above) to the version of the GeoServer instance.

 #. Extract the files in this archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.
 
    .. warning:: this plugin will install a version of the `H2 <http://www.h2database.com>`__  library that **is not compatible** 
                 with other plugins using H2 (e.g. grib/netcdf).   

                 This H2 library is purely for demo purposes, allowing you to run the GeoFence plugin without the need to configure an external DB backend.  

                 You should **remove the h2 library** file (see section :ref:`Clean up H2 dependency<Clean up H2 dependency>`) 
                 and follow the steps in :ref:`Configure GeoFence to use PostgreSQL <Configure GeoFence to use PostgreSQL>`
                 in order to be able to use the grib/netcdf extensions.
 
 #. Add the following system variable among the JVM startup options (location varies depending on installation type): ``-Dgwc.context.suffix=gwc`` to avoid conflicts with GWC pages.

 #. Restart GeoServer


Configure the plugin
--------------------

By default, GeoFence is configured to use H2 as a backend database and will work out of the box with the provided configuration.

As reported above, you are strongly encouraged to move to PostgreSQL/PostGIS.


.. _Configure GeoFence to use PostgreSQL:

Configure GeoFence to use PostgreSQL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
In order to instruct GeoFence to use PostgreSQL, you need to create the 
file :file:`<DATADIR>/geofence/geofence-datasource-ovr.properties` with a content like this:

.. code-block:: properties
   
    geofenceVendorAdapter.databasePlatform=org.hibernate.spatial.dialect.postgis.PostgisDialect
    geofenceDataSource.driverClassName=org.postgresql.Driver
    geofenceDataSource.url=jdbc:postgresql://<HOST>:<PORT>/<DATABASE>
    geofenceDataSource.username=<USERNAME>
    geofenceDataSource.password=<PASSWORD>
    geofenceEntityManagerFactory.jpaPropertyMap[hibernate.default_schema]=<SCHEMA>

    # avoid hibernate transaction issues
    geofenceDataSource.testOnBorrow=true
    geofenceDataSource.validationQuery=SELECT 1
    geofenceEntityManagerFactory.jpaPropertyMap[hibernate.testOnBorrow]=true
    geofenceEntityManagerFactory.jpaPropertyMap[hibernate.validationQuery]=SELECT 1


.. note:: The `PostgisDialect` is deprecated and should be replaced according to the PostgreSQL version used.
    Please use the proper dialect as reported in the `hibernate summary page <https://docs.jboss.org/hibernate/orm/5.6/javadocs/org/hibernate/spatial/dialect/postgis/package-summary.html>`__

.. note:: By default GeoFence will create the initial schema or update the DB schema by itself when needed.
          In case you want to manage the schema by yourself, you may want to use the SQL file located
          `here <https://github.com/geoserver/geofence/tree/main/doc/setup/sql>`__

          Also, you need to set this property to `validate` (default value is `update`).

          .. code-block:: properties   

              geofenceEntityManagerFactory.jpaPropertyMap[hibernate.hbm2ddl.auto]=validate


Note that the jar files needed to use PostgreSQL and PostGIS are already in the zip file of the plugin.


.. _Clean up H2 dependency:

Clean up H2 dependency
^^^^^^^^^^^^^^^^^^^^^^

As soon as you move to another backend DB, do remember to remove the file `h2-<version>.jar` from the :file:`WEB-INF/lib` directory.


Other info
^^^^^^^^^^

You may found other info about configuration in this `GeoFence wiki page <https://github.com/geoserver/geofence/wiki/GeoFence-configuration>`__ .