---
render_macros: true
---


# OGC Testbed Experiments

The following module is part of the OGC Testbed experiments.

## DGGS

The DGGS functionality exposes data structured as DGGS, based on either H2 or rHealPix, based on engineering reports of [Testbed 18](https://docs.ogc.org/per/20-039r2.md).

To make full usage of the DGGS functionality, a ClickHouse installation is also needed.

To install these modules:

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Development** tab, and locate the nightly release that corresponds to the GeoServer you are running.

    Follow the **Community Modules** link and download `ogcapi-dggs` zip archive.

    - {{ snapshot }} example: [ogcapi-dggs](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-ogcapi-dggs-plugin.zip)

    The website lists active nightly builds to provide feedback to developers, you may also [browse](https://build.geoserver.org/geoserver/) for earlier branches.

3.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

    !!! warning
        Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example geoserver-{{ snapshot }}-ogcapi-dggs-plugin.zip above).

4.  Restart GeoServer.

    On restart the services are listed at <http://localhost:8080/geoserver>

The DGGS datastore can read data from a ClickHouse database. In case you want to use JNDI along with Tomcat, here is a sample data source declaration for the `etc/context.xml` file:

```xml
<Resource name="jdbc/clickhouse" auth="Container" type="javax.sql.DataSource"
            maxTotal="100" maxIdle="30" maxWaitMillis="10000"
            username="myUser" password="myPassword" driverClassName="com.clickhouse.jdbc.ClickHouseDriver"
            url="jdbc:clickhouse://localhost:8123/theDatabase"
            connectionProperties="clickhouse_setting_max_query_size=1000000;socket_timeout=300000;jdbc_ignore_unsupported_values=true;typeMappings=DateTime=java.time.ZonedDateTime,DateTime64=java.time.ZonedDateTime"/>
```

When connecting directly, the DGGS datastore sets these connection properties on its own. A JNDI data source is used as configured, so **it must declare them itself**:

| Property | Value | Without it |
|---|---|---|
| `typeMappings` | `DateTime=java.time.ZonedDateTime,DateTime64=java.time.ZonedDateTime` | `DateTime` and `DateTime64` values are shifted by the offset between the GeoServer and the ClickHouse server time zones |
| `jdbc_ignore_unsupported_values` | `true` | Reads made within a transaction fail, as the ClickHouse JDBC driver does not support transactions |
| `clickhouse_setting_max_query_size` | `1000000` | Queries with many zone ids can exceed the ClickHouse default maximum query size and fail |
| `socket_timeout` | `300000` (milliseconds) | Long-running aggregation queries can time out |

It is to be noted that the ClickHouse JDBC driver rejects unknown connection properties: **the connection fails if any of them is misspelled**. ClickHouse server settings, such as `max_query_size`, must be prefixed with `clickhouse_setting_`. The `http_connection_provider` URL parameter used in older setups is no longer needed and is ignored.
