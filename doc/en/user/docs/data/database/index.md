# Databases

This section discusses the database data sources that GeoServer can access.

The standard GeoServer installation supports accessing the following databases:

<div class="grid cards" markdown>

- [DataDatabasePostgis](postgis.md)
- [DataDatabaseH2](h2.md)

</div>

Other data sources are supplied as GeoServer extensions. Extensions are downloadable modules that add functionality to GeoServer. Extensions are available at the [GeoServer download page](https://geoserver.org/download).

!!! warning

    The extension version must match the version of the GeoServer instance.

<div class="grid cards" markdown>

- [DataDatabaseDb2](db2.md)
- [DataDatabaseMysql](mysql.md)
- [DataDatabaseOracle](oracle.md)
- [DataDatabaseSqlserver](sqlserver.md)

</div>

GeoServer provides extensive facilities for controlling how databases are accessed. These are covered in the following sections.

<div class="grid cards" markdown>

- [DataDatabaseConnection Pooling](connection-pooling.md)
- [DataDatabaseJndi](jndi.md)
- [DataDatabaseSqlview](sqlview.md)
- [DataDatabasePrimarykey](primarykey.md)
- [DataDatabaseSqlsession](sqlsession.md)

</div>
