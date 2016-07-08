.. _community_gwc_sqlite:

GWC SQLite Plugin
=================

This plugin provides integration with GWC SQLite based blob stores. At the moment only one blob store of this type is available, the MBTiles blob store.


MBTiles Blob Store
++++++++++++++++++

This blob store allow us to store tiles using the `MBTiles <https://github.com/mapbox/mbtiles-spec/blob/master/1.1/spec.md>`_ specification (version 1.1) which defines a schema for storing tiles in an `SQLite <https://www.sqlite.org/>`_ database with some restrictions regarding tiles formats and projections.

MBTiles specification only supports JPEG and PNG formats and projection EPSG:3857 is assumed. The implemented blob store will read and write MBTiles files compliant with the specification but will also be able to write and read MBTiles files that use others formats and projections.

Using the MBTiles blob store will bring several benefits at the cost of some performance loss. The MBTiles storage uses a significantly smaller number of files, which results in easier data handling (e.g., backups, moving tiles between environments). In some cases the stored data will be more compact reducing the size of the data on disk.

When compared to the file blob store this store has two limitations:

* This store does not integrate with disk quota, this is a consequence of using database files.
* **This store cannot be shared among several GeoWebCache instances.**

.. note:: If disk quota is activated the stored stats will not make much sense and will not reflect the actual disk usage, the size of the database files cannot be really controlled.

Database files cannot be managed as simple files. When connections to a database are open the associated file should not be deleted, moved or switched or the database file may become corrupted. Databases files can also become fragmented after deleting an huge amount of data or after frequent inserts, updates or delete operations.

File Path Templates
````````````````````

An MBTiles file will correspond to an SQLite database file. In order to limit the amount of contention on each single database file users will be allowed to decide the granularity of the databases files. When GeoWebCache needs to map a tile to a database file it will only look at the databases files paths, it will not take in account the MBTiles metadata (this is why this store is able to handle others formats and projections).

To configure the databases files granularity the user needs to provide a file path template. The default file path template for the MBTiles blob store is this one:

.. code-block:: none

  {layer}/{grid}{format}{params}/{z}-{x}-{y}.sqlite

This file template will stores all the tiles belonging to a certain layer in a single folder that will contain sub folders for each given format, projection and set of parameters and will group tiles with the same zoom level, column range and row range in a SQLite file. The column and row range values are passed by configuration, by default those values are equal to 250. The provided files paths templates will always be considered relative to the root directory provided as a configuration option.

Follows an example of what the blob store root directory structure may look like when using the default path template:

.. code-block:: none

  .
  |-- nurc_Pk50095
  |   `-- EPSG_4326image_pngnull
  |       |-- 11_2000_1500.sqlite
  |       `-- 12_4250_3000.sqlite
  `-- topp_states
      |-- EPSG_900913image_jpeg7510004a12f49fdd49a2ba366e9c4594be7e4358
      |   |-- 6_250_500.sqlite
      |   `-- 7_0_0.sqlite
      `-- EPSG_900913image_jpegnull
          |-- 3_500_0.sqlite
          |-- 4_0_250.sqlite
          `-- 8_750_500.sqlite

If no parameters were provided *null* string will be used. Is the responsibility of the user to define a file path template that will avoid collisions.

The terms that can be used in the file path template are:

* **grid**: the grid set id
* **layer**: the name of the layer
* **format**: the image format of the tiles
* **params**: parameters unique hash
* **x**: column range, computed based on the column range count configuration property
* **y**: row range, computed based on the row range count configuration property
* **z**: the zoom level

It is also possible to use parameters values, like *style* for example. If the parameter is not present *null* will be used.

.. note:: Characters ``\`` and ``/`` can be used as path separator, they will be translated to the operating system specific one (``\`` for Linux and ``/`` for Windows). Any special char like ``\``, ``/``, ``:`` or empty space used in a term value will be substituted with an underscore.

MBTiles Metadata
`````````````````

A valid MBTiles file will need some metadata, the image format and layer name will be automatically added when an MBTiles file is created. The user can provide the remaining metadata using a properties file whose name must follow this pattern:

.. code-block:: none

  <layerName>.metadata

As an example, to add metadata ``description`` and ``attribution`` entries to layer ``tiger_roads`` a file named ``tiger_roads.properties`` with the following content should be present in the metadata directory:

.. code-block:: none

  description=ny_roads
  attribution=geoserver

The directory that contains this metadata files is defined by a configuration property.

Expiration Rules
`````````````````

The MBTiles specification don't give information about when a tile was created. To allow expire rules, an auxiliary table is used to store tile creation time. In the presence of an MBTiles file generated by a third party tool it is assumed that the creation time of a tile was the first time it was accessed. This feature can be activated or deactivated by configuration. Note that this will not break the MBTiles specification compliance.

Eager Truncate
```````````````

When performing a truncate of the cache the store will try to remove the whole database file avoiding to create fragmented space. This is not suitable for all the situations and is highly dependent on the database files granularity. The configuration property ``eagerDelete`` allows the user to disable or deactivate this feature which is disabled by default. 

When a truncate request by tile range is received all the the databases files that contains tiles that belong to the tile range are identified. If eager delete is set to true those databases files are deleted otherwise a single delete query for each file is performed.

Configuration Example
``````````````````````

Follows as an example the default configuration of the MBTiles store:

.. figure:: img/mbtilesBlobStore.png

The *rootDirectory* property defines the location where all the files produced by this store will be created. The *templatePath* property is used to control the granularity of the database files (see section above). Properties *rowRangeCount* and *columnRangeCount* will be used by the path template to compute tile ranges.

The *poolSize* property allows to control the max number of open database files, when defining this property the user should take in account the number open files allowed by the operating system. The *poolReaperIntervalMs* property controls how often the pool size will be checked to see if some database files connections need to be closed.

Property *eagerDelete* controls how the truncate operation is performed (see section above). The property *useCreateTime* can be used to activate or deactivate the insertion of the tile creation time (see section above). Property *executorConcurrency* controls the parallelism used to perform certain operations, like the truncate operation for example. Property *mbtilesMetadataDirectory* defines the directory where the store will look for user provided MBTiles metadata.

.. note:: Since the connection pool eviction happens at a certain interval, it means that the number of files open concurrently can go above the threshold limit for a certain amount of time.

Replace Operation
``````````````````

As said before, if the cache is running SQLite files cannot be simply switched, first all connections need to be closed. The replace operation was created for this propose. The replace operation will first copy the new file side by side the old one, then block the requests to the old file, close the connections tot he old file, delete the old one, rename the new file to current one, reopen the new db file and start serving requests again. Should be almost instant.

A REST entry point for this operation is available, it will be possible to submit a ZIP file or a single file along with the request. The replace operation can also use an already present file or directory. When using a directory the directory structure will be used to find the destination of each file, all the paths will be assumed to be relative to the store root directory. This means that is possible to replace a store content with another store content (a seeded one for example) by zipping the second store root directory and send it as a replacement.

.. note:: When using a local directory or submitting a zip file all the file present in the directory will be considered.

There is four ways to invoke this operation. Follows an example of all those variants invocations using CURL.

Replace a single file uploading the replacement file:

.. code-block:: none

  curl -u admin:geoserver -H 'Content-Type: multipart/form-data'
    -F "file=@tiles_0_0.sqlite"
    -F "destination=EPSG_4326/sf_restricted/image_png/null/10/tiles_0_0.sqlite"
    -F "layer=sf:restricted"
    -XPOST 'http://localhost:8080/geoserver/gwc/rest/sqlite/replace'

Replace a single file using a file already present on the system:

.. code-block:: none

  curl -u admin:geoserver -H 'Content-Type: multipart/form-data'
    -F "source=/tmp/tiles_0_0.sqlite"
    -F "destination=EPSG_4326/sf_restricted/image_png/null/10/tiles_0_0.sqlite"
    -F "layer=sf:restricted"
    -XPOST 'http://localhost:8080/geoserver/gwc/rest/sqlite/replace'

Replace multiple files uploading a ZIP file:

.. code-block:: none

  curl -u admin:geoserver -H 'Content-Type: multipart/form-data'
    -F "file=@tiles.zip"
    -F "layer=sf:restricted"
    -XPOST 'http://localhost:8080/geoserver/gwc/rest/sqlite/replace'

Replace multiple files using a directory already present on the system:

.. code-block:: none

  curl -u admin:geoserver -H 'Content-Type: multipart/form-data'
    -F "source=/tmp/tiles"
    -F "layer=sf:restricted"
    -XPOST 'http://localhost:8080/geoserver/gwc/rest/sqlite/replace'

The *layer* parameter identifies the layer whose associated blob store content should be replaced. The *file* parameter is used to upload a single file or a ZIP file. The *source* parameter is used to reference an already present file or directory. The *destination* parameter is used to define the file that should be replaced with the provided file.

This are the only valid combinations of this parameters other combinations will ignore some of the provided parameters or will throw an exception.