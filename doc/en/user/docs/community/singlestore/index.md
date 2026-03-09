---
render_macros: true
---


# SingleStore

!!! warning

    Currently the SingleStore extension is a community module. While still usable, do not expect the same reliability as with supported extensions.

[SingleStore](https://www.singlestore.com) is an open source relational database with some [limited spatial functionality](https://docs.singlestore.com/cloud/developer-resources/functional-extensions/working-with-geospatial-features). In particular, it supports only `GEOGRAPHY` data types, does not support multipart geometries, and has a specialized `GEOGRAPHYPOINT` type that is used to store points with higher performance than the generic GEOGRAPHY type. Spatial functionality is limited to simple search operations, such as intersects, contains, and within distance.

## Installing the SingleStore extension {: #singlestore_install }

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download ``singlestore`` zip archive.

    - {{ snapshot }} example: [singlestore](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-singlestore-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory of the GeoServer installation.

## Adding a SingleStore database

Once the extension is properly installed `SingleStore` will show up as an option when creating a new data store.

![](images/singlestorecreate.png)
*SingleStore in the list of data sources*

## Configuring a SingleStore data store

![](images/singlestoreconfigure.png)

![](images/singlestoreconfigure2.png)
*Configuring a SingleStore data store*

| `host` | The SingleStore server host name or ip address. |
|----|----|
| `port` | The port on which the SingleStore server is accepting connections. |
| `database` | The name of the database to connect to. Can also contain a suffix with a connection URL query, such as ``mydbname?useSSL=false`` |
| `user` | The name of the user to connect to the SingleStore database as. |
| `password` | The password to use when connecting to the database. Left blank for no password. |
| `max connections` `min connections` `validate connections` | Connection pool configuration parameters. See the [Database Connection Pooling](../../data/database/connection-pooling.md) section for details. |
