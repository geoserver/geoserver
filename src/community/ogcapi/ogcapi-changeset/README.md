Changeset API
-------------

Prototype implementation of the Tiles Changeset extension from Testbed 15, allows
to get a package with the tiles modified in a tile cache after a image mosaic
has been modified using the Images API.

Configuring a database
----------------------

The system will use a local H2 database located in the ``${GEOSERVER_DATA_DIR}/changeset`` directory unless otherwise configured. 

It's possible to configure it by adding a ``${GEOSERVER_DATA_DIR}/changeset-store.properties``
configuration file with the same properties normally provided via the UI, for example:

```
dbtype=postgis
host=localhost
port=5434
database=dbname
schema=public
user=usr
passwd=psw
Loose\ bbox=true
Estimated\ extends=false
validate\ connections=true
Connection\ timeout=10
preparedStatements=true
```    