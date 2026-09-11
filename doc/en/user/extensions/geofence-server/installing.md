---
render_macros: true
---


# Install the plugin

!!! warning
    the plugins `geofence-server` and `geofence` are about two different architectural configurations. 
    Please install either one according to your setup.

    It is recommended to **not** install both of them at the same time.    
    
    `geofence-server` will run the GeoFence engine internally, and you won't need an external GeoFence webapp.


## Download the plugin

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

1.  Visit the [website download](https://geoserver.org/download) page, change the **Archive** tab, and locate your release.

    From the list of **Security** extensions download **GeoFence Server**:

    - {{ release }}: [geoserver-{{ release }}-geofence-server-plugin.zip](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/extensions/geoserver-{{ release }}-geofence-server-plugin.zip)
    - {{ snapshot }}: [geoserver-{{ snapshot }}-geofence-server-plugin.zip](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ snapshot }}-geofence-server-plugin.zip)

    Make sure to match the plugin version (e.g. {{ release }} above) to the version of the GeoServer instance.

## Install the files

1.  Extract the files in this archive to the **`WEB-INF/lib`** directory of your GeoServer installation.
1.  [Configure the plugin](#configure-the-plugin)
1.  Restart GeoServer

## Configure the plugin {: #configure-the-plugin }

You need a properties file containing the information to connect to the DB where GeoFence will store its data.

If the file is not present, the problem will be logged out to the GeoServer log:
```
01 set 17:24:13 WARN   [config.GeofencePersistenceConfig] - GeoFence embedded engine will be unavailable until this is fixed
java.lang.IllegalStateException: No geofence datasource configuration found. Checked:
  - <your geoserver data dir>/data/geofence/geofence-datasource.properties
  - <container working dir>/geofence-datasource.properties
Wrote a sample file to <your geoserver data dir>/data/geofence/geofence-datasource.properties.sample - copy it to geofence-datasource.properties in the same directory and fill in real credentials.
    at org.geofence.core.db.config.DatasourcePropertiesLoader.load(DatasourcePropertiesLoader.java:79)
    at org.geofence.core.db.config.DatasourcePropertiesLoader.load(DatasourcePropertiesLoader.java:57)
    at org.geofence.core.db.config.GeofencePersistenceConfig.<init>(GeofencePersistenceConfig.java:48)
    <long stacktrace here>
```

and, as reported in the log, a sample file will be created for you. You need to copy the sample into the file **`<DATADIR>/geofence/geofence-datasource.properties`** and edit it with the real info.


This is the sample file content:

```properties
# Sample GeoFence datasource configuration.
#
# Copy this file to the same name without the ".sample" suffix, in the same directory, and
# fill in real credentials. All four properties are required; there is no built-in default.

geofence.datasource.url=jdbc:postgresql://localhost:5432/geofence
geofence.datasource.username=geofence
# Plain text here. When GeoFence runs embedded in GeoServer, the value may instead be encrypted with
# GeoServer's config-password encryption (same scheme as store connection passwords) and is decrypted
# transparently on startup. To have a clear-text password encrypted at rest, prefix it with 'plain:'
# (e.g. plain:mysecret): on next startup GeoFence encrypts it and rewrites this line with the result.
geofence.datasource.password=geofence
geofence.datasource.driver=org.postgresql.Driver

# Optional: any geofence.hibernate.* property is passed through to Hibernate/JPA, with the
# prefix stripped, e.g.:
# geofence.hibernate.hbm2ddl.auto=validate
# geofence.hibernate.default_schema=public

# Optional: any geofence.datasource.hikari.* property is passed through to the connection pool
# (HikariCP), with the prefix stripped - must be a real Hikari property name, an unrecognized one
# fails fast at startup. keepaliveTime periodically pings idle pooled connections so a dead one
# (e.g. after a DB restart) is detected and evicted instead of causing a transaction failure later.
# geofence.datasource.hikari.keepaliveTime=30000
```


!!! note
    By default GeoFence will create the initial schema or update the DB schema by itself when needed. In case you want to manage the schema by yourself, you may want to use the SQL file located [here](https://github.com/geoserver/geofence/tree/main/doc/setup/sql)
    
    Also, you need to set this property to `validate` (default value is `update`).
    
    ``` properties
    geofence.hibernate.hbm2ddl.auto=validate
    ```

### Other info

You may found other info about configuration in this [GeoFence wiki page](https://github.com/geoserver/geofence/wiki/GeoFence-configuration) .
