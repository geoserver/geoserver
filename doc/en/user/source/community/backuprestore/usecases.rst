.. _backup_restore_usecases:

Use Cases
=========

Database vs. Shapefile based indexer
------------------------------------

When using a DataBase as backend storage for the mosaic index, a ``datastore.properties`` file is present on the mosaic folder containing the connection parameters.

In case the user wants to parametrize this, he must create a ``.template`` datastore properties file containing all the properties of the original one but using placemarks as parametric values.

As an instance:

    ::
    
        host=${mosaic1.jdbc.host}
        port=${mosaic1.jdbc.port}
        ...

The backup and restore extension will save on the archive both the original ``.properties`` and the ``.template``

When restoring, the extension will overwrite the ``.properties`` by using the ``.template`` and substituting the placemarks with the correct environment property values.

When using a shapefile as backend for the index the shapefile itself will be created once again by the mosaic when performing the first harvest operation.

Database Connection Parameters vs. JNDI
---------------------------------------

This use case is similar to the previous one, except for the fact that instead of parameters like host and port we will have a parametric JNDI name.

Indexer files and regex
-----------------------

The approach will be exactly the same of the ``datastore.properties``.

Is is worth notice that the backup extension will overwrite only the files having a corresponding ``.template`` prototype.

Granules stored on the same mosaic folder vs. absolute path
-----------------------------------------------------------

This won't impact the backup and restore at all, since it will never dumps data into the final archive.

It is important, however, that the absolute paths are parametric similar to the connection parameters explained above.

Dealing with non-existing indexes on the target restored environment
--------------------------------------------------------------------

It is possible that when restoring the ImageMosaic the index does not exist on the target environment.

The backup and restore extension should perform a double check once restored the ``datastore.properties`` file trying to access the index store. 

#. In case of failure, i.e. the extension cannot connect to the datastore, the resource will fail.

#. In case the datastore is accessible but the index does not exist, the plugin will create an empty mosaic on the catalog instead of failing.

