# Databases

This section discusses the database data sources that GeoServer can access.

The standard GeoServer installation supports accessing the following databases:

<div class="grid cards" markdown>

-   [PostGIS](postgis.md)
-   [H2](h2.md)

</div>

Other data sources are supplied as GeoServer extensions. Extensions are downloadable modules that add functionality to GeoServer. Extensions are available at the [GeoServer download page](https://geoserver.org/download).

!!! warning

    The extension version must match the version of the GeoServer instance.

<div class="grid cards" markdown>

-   [Db2](db2.md)
-   [MySQL](mysql.md)
-   [Oracle](oracle.md)
-   [Microsoft SQL Server and SQL Azure](sqlserver.md)

</div>

GeoServer provides extensive facilities for controlling how databases are accessed. These are covered in the following sections.

<div class="grid cards" markdown>

-   [Database Connection Pooling](connection-pooling.md)
-   [JNDI](jndi.md)
-   [SQL Views](sqlview.md)
-   [Controlling feature ID generation in spatial databases](primarykey.md)
-   [Custom SQL session start/stop scripts](sqlsession.md)

</div>
