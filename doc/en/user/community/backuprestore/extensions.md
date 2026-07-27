# ImageMosaic indexer extension

!!! warning
    Backup and Restore is a community module. It is usable, but does not have the same support guarantees as official extensions.

## Introduction

*ImageMosaic coverage stores* rely on several `.properties` files that tell the reader how to build the mosaic index. This extension point lets the Backup and Restore module **inject environment properties** into those indexers, so an ImageMosaic can be ported automatically between environments. The end-user side of this is described in [the ImageMosaic indexer use cases](usecases.md#imagemosaic-indexer-parameterization).

## Extension points

The module exposes a read/write extension point for handling additional resources attached to a catalog item (typically a `ResourceInfo`):

```java
public interface CatalogAdditionalResourcesWriter<T> {

    boolean canHandle(Object item);

    void writeAdditionalResources(Backup backupFacade, Resource base, T item) throws IOException;
}

public interface CatalogAdditionalResourcesReader<T> {

    boolean canHandle(Object item);

    void readAdditionalResources(Backup backupFacade, Resource base, T item) throws IOException;
}
```

Implementations are contributed as GeoServer extension beans and discovered through `GeoServerExtensions`. The module dispatches to every registered handler whose `canHandle(item)` returns `true`:

- On **backup**, the abstract `CatalogWriter.firePostWrite(...)` runs every `CatalogAdditionalResourcesWriter` after the item's configuration has been written. It is invoked from `CatalogFileWriter`, which writes catalog items as flat XML files to the archive folder.
- On **restore**, the abstract `CatalogReader.firePostRead(...)` runs every `CatalogAdditionalResourcesReader` after the item has been read back. It is invoked from `CatalogFileReader`, which reads catalog items from the archive into the in-memory restore catalog.

Two writers ship with the module: `StyleInfoAdditionalResourceWriter` (style sidecar files) and `ResourceInfoAdditionalResourceWriter` (Freemarker templates and schema mappings).

!!! note
    The `CatalogItemWriter` and `CatalogItemReader` (used when persisting directly to or from the live catalog) do **not** invoke this extension point. The additional-resource SPI fires only on the file-based path — `CatalogFileWriter` / `CatalogFileReader` — which is the one used for archive backup and restore.

## Behaviour for ImageMosaic indexers

The goal is to let the additional-resource handlers:

1. restore the ImageMosaic indexer `.properties`, injecting environment properties from the matching `.template`; and
2. check whether the mosaic index physically exists and, if not, create an empty one.

On a **backup** operation:

1. The additional-resource writer checks that the item is an ImageMosaic coverage store.
2. It looks for `*.template` files in the ImageMosaic index directory (read from the coverage store).
3. It stores the `*.template` files alongside the `*.properties` files in the target backup folder.

On a **restore** operation:

1. The additional-resource reader checks that the item is an ImageMosaic coverage store.
2. It looks for the `*.template` files and restores them to the path read from the coverage store configuration.
3. It overwrites the `*.properties` files by resolving the environment properties declared in the templates.
4. It checks whether an empty mosaic needs to be created.

!!! note
    The engine was reworked for GeoServer 3.x: it runs on Spring Batch 6 / Spring 7, and the job graphs are defined in Java `@Configuration` (`BackupJobConfiguration`, `RestoreJobConfiguration`, `BatchInfrastructureConfiguration`) rather than `applicationContext.xml`. Custom steps, tasklets, readers, processors and writers should be wired the same way. See the developer note in [what changed between 2.x and 3.x](migration.md#for-developers-and-extension-authors).
