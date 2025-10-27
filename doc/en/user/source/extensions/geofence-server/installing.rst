.. _geofence_server_install:

Installing the GeoServer GeoFence Server extension
==================================================

.. warning:: the plugins `geofence-server` and `geofence` should **not** be both installed at the same time.  

             Please install either one according to your setup.  

             `geofence-server` will run the GeoFence engine internally, and you won't need an external GeoFence webapp.

Select the plugin you need 
--------------------------

GeoFence Server extension is provided as two mutually exclusive packages, to be used according to your setup:

- GeoFence Server PostgreSQL: (strongly recommended choice) contains all the libraries to run geofence-server, using as backend an externally configured PostgreSQL DB.

  
- GeoFence Server H2: (Quick demo choice) contains all the libraries to run geofence-server, using as backend an embedded H2 DB.

  .. warning:: this plugin will install a version of the `H2 <http://www.h2database.com>`__  library that **is not compatible** with other plugins using H2 (e.g. grib/netcdf).  
 
     This package is purely for demo purposes, allowing you to run the GeoFence plugin without the need to configure an external DB backend.  

Install the plugin
------------------

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   Recommended: From the list of **Security** extensions download **GeoFence Server (Postgres)**:
   
   * |release| :download_extension:`geofence-server-postgres`
   * |version| :nightly_extension:`geofence-server-postgres`
   
   Quick Demo: From the list of **Security** extensions download **GeoFence Server (H2)**:
   
   * |release| :download_extension:`geofence-server-h2`
   * |version| :nightly_extension:`geofence-server-h2`
     
   Make sure to match the plugin version (e.g. |release| above) to the version of the GeoServer instance.

 #. Extract the files in this archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.
  
 #. Add the following system variable among the JVM startup options (location varies depending on installation type): ``-Dgwc.context.suffix=gwc`` to avoid conflicts with GWC pages.

 #. :ref:`Configure the plugin<Configure the plugin>`

 #. Restart GeoServer


.. _Configure the plugin:

Configure the plugin
--------------------

H2 configuration
^^^^^^^^^^^^^^^^
If you are using the H2 flavour of the plugin, you don't need to configure anything.   
By default, GeoFence will use H2 as a backend database and will work out of the box with the internal default configuration.

As reported above, you are strongly encouraged to move to PostgreSQL/PostGIS.


PostgreSQL configuration
^^^^^^^^^^^^^^^^^^^^^^^^
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


Other info
^^^^^^^^^^

You may found other info about configuration in this `GeoFence wiki page <https://github.com/geoserver/geofence/wiki/GeoFence-configuration>`__ .

