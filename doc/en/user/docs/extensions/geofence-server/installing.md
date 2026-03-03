---
render_macros: true
---

# Installing the GeoServer GeoFence Server extension

!!! warning

    the plugins ``geofence-server`` and ``geofence`` should **not** be both installed at the same time.
    
    Please install either one according to your setup.
    
    ``geofence-server`` will run the GeoFence engine internally, and you won't need an external GeoFence webapp.

## Select the plugin you need

GeoFence Server extension is provided as two mutually exclusive packages, to be used according to your setup:

- [geofence-server-postgres](https://build.geoserver.org/geoserver/main/ext-latest/geofence-server-postgres): (strongly recommended choice) contains all the libraries to run geofence-server, using as backend an externally configured PostgreSQL DB.

- [geofence-server-h2](https://build.geoserver.org/geoserver/main/ext-latest/geofence-server-h2): contains all the libraries to run geofence-server, using as backend an embedded H2 DB.

  > :::: warning
  > ::: title
  > Warning
  > :::
  >
  > this plugin will install a version of the [H2](http://www.h2database.com) library that **is not compatible** with other plugins using H2 (e.g. grib/netcdf).
  >
  > This package is purely for demo purposes, allowing you to run the GeoFence plugin without the need to configure an external DB backend.
  > ::::

## Install the plugin

> 1.  Visit the [website download](https://geoserver.org/download) page, locate your release, and download either one of the packages listed above.
>
>     The download link will be in the **Extensions** section under **Other**.
>
>     Make sure to match the plugin version (e.g. {{ release }} above) to the version of the GeoServer instance.
>
> 2.  Extract the files in this archive to the **`WEB-INF/lib`** directory of your GeoServer installation.
>
> 3.  Add the following system variable among the JVM startup options (location varies depending on installation type): `-Dgwc.context.suffix=gwc` to avoid conflicts with GWC pages.
>
> 4.  **Configure the plugin**
>
> 5.  Restart GeoServer

## Configure the plugin {: #Configure the plugin }

### H2 configuration

If you are using the H2 flavour of the plugin, you don't need to configure anything. By default, GeoFence will use H2 as a backend database and will work out of the box with the internal default configuration.

As reported above, you are strongly encouraged to move to PostgreSQL/PostGIS.

### PostgreSQL configuration

In order to instruct GeoFence to use PostgreSQL, you need to create the file **`<DATADIR>/geofence/geofence-datasource-ovr.properties`** with a content like this:

``` properties
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
```

!!! note

    The ``PostgisDialect`` is deprecated and should be replaced according to the PostgreSQL version used. Please use the proper dialect as reported in the [hibernate summary page](https://docs.jboss.org/hibernate/orm/5.6/javadocs/org/hibernate/spatial/dialect/postgis/package-summary.html)

!!! note

    By default GeoFence will create the initial schema or update the DB schema by itself when needed. In case you want to manage the schema by yourself, you may want to use the SQL file located [here](https://github.com/geoserver/geofence/tree/main/doc/setup/sql)
    
    Also, you need to set this property to ``validate`` (default value is ``update``).
    
    ``` properties
    geofenceEntityManagerFactory.jpaPropertyMap[hibernate.hbm2ddl.auto]=validate
    ```

### Other info

You may found other info about configuration in this [GeoFence wiki page](https://github.com/geoserver/geofence/wiki/GeoFence-configuration) .
