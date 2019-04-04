.. _backup_restore_extensions:

Backup and Restore Extension for the management of ImageMosaic indexers
=======================================================================

Introduction
------------

*ImageMosaics CoverageStores* make use of several ``.properties`` files instructing the reader on how to create the mosaic index.

What we want to achieve is to allow the GeoServer Backup & Restore module to *inject* environment properties on indexers allowing the ImageMosaic to be automatically ported among different environments.

Technical Details
-----------------

The GeoServer Backup & Restore module actually provides an extension point on reading / writing allowing GeoServer to handle additional resources related to a particular ``ResourceInfo``.

    The interfaces ::

        public interface CatalogAdditionalResourcesWriter<T> {

            public boolean canHandle(Object item);

            public void writeAdditionalResources(Backup backupFacade, Resource base, T item)
                    throws IOException;

        }

    ::

        public interface CatalogAdditionalResourcesReader<T> {

            public boolean canHandle(Object item);

            public void readAdditionalResources(Backup backupFacade, Resource base, T item)
                    throws IOException;

        }

Is invoked by the ``CatalogFileWriter`` (when doing a Backup) and the ``CatalogItemWriter`` (when doing a Restore) after a successful write of the resource configuration on the, respectively, target backup folder and in-memory catalog.

The idea is the following one *allowing the CatalogItemWriter to*:

#. Restore the ImageMosaic Indexer Properties injecting environment properties

#. Check if the Mosaic index physically exist and if not create an empty one

In order to do that we envisage the following technical approach

On a **BACKUP** operation

    #. The Additional Resource Writer checks if the ``ResourceInfo`` is an ImageMosaic Coverage Store.

    #. The Additional Resource Writer looks for ``*.template`` files on the ImageMosaic index directory. It must store them into the zip archive by reading the path from the Coverage Store.

    #. The Additional Resource Writer stores the ``*.template`` along with the ``*.properties`` files on the target backup folder. Same as above.


On a **RESTORE** operation

    #. The Additional Resource Reader checks if the ``ResourceInfo`` is an ImageMosaic Coverage Store.

    #. The Additional Resource Reader looks for ``*.template`` files on the ImageMosaic index directory. It will try to restore them by using the path read from the Coverage Store configuration.

    #. The Additional Resource Reader overwrites the ``*.properties`` files by resolving all the environment properties declared on the templates.

    #. The Additional Resource Reader checks if the empty mosaic must be created or not.

