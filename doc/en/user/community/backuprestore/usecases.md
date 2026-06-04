# Use Cases

## Database vs. Shapefile based indexer

When using a DataBase as backend storage for the mosaic index, a `datastore.properties` file is present on the mosaic folder containing the connection parameters.

In case the user wants to parametrize this, he must create a `.template` datastore properties file containing all the properties of the original one but using placemarks as parametric values.

As an instance:

```
host=${mosaic1.jdbc.host}
    port=${mosaic1.jdbc.port}
    ...
```

The backup and restore extension will save on the archive both the original `.properties` and the `.template`

When restoring, the extension will overwrite the `.properties` by using the `.template` and substituting the placemarks with the correct environment property values.

When using a shapefile as backend for the index the shapefile itself will be created once again by the mosaic when performing the first harvest operation.

## Database Connection Parameters vs. JNDI

This use case is similar to the previous one, except for the fact that instead of parameters like host and port we will have a parametric JNDI name.

## Indexer files and regex

The approach will be exactly the same of the `datastore.properties`.

It's worth notice that the backup extension will overwrite only the files having a corresponding `.template` prototype.

## Granules stored on the same mosaic folder vs. absolute path

This won't impact the backup and restore at all, since it will never dumps data into the final archive.

It is important, however, that the absolute paths are parametric similar to the connection parameters explained above.

## Dealing with non-existing indexes on the target restored environment

It is possible that when restoring the ImageMosaic the index does not exist on the target environment.

The backup and restore extension should perform a double check once restored the `datastore.properties` file trying to access the index store.

1.  In case of failure, i.e. the extension cannot connect to the datastore, the resource will fail.
2.  In case the datastore is accessible but the index does not exist, the plugin will create an empty mosaic on the catalog instead of failing.

## Migrating a catalog to another GeoServer instance

The default backup archive is **portable**: catalog object ids are stripped and objects reference each other by name. This is the right format when the archive is restored into a fresh, empty data directory — the restore re-creates everything and assigns new ids.

When the goal is instead to **merge** the contents of one instance into another, already-populated instance (a migration, a consolidation of several servers, or a promotion from staging to production), the name-based format has two limitations:

- A restore matches incoming objects against the target **by name**, so an object that happens to share a name with an existing one is treated as the same object.
- GeoWebCache keys its tile-layer configurations strictly by the **published id** of the layer/layer group (there is no name fallback), so name-based archives cannot reliably re-link the cached tile layers.

To preserve the original identities, take the backup with the `BK_PRESERVE_IDS` option (see [Backup options](usagerest.md)):

```json
{
   "backup":{
      "archiveFile":"/home/sg/BackupAndRestore/migration.zip",
      "overwrite":true,
      "options":{
        "option": ["BK_PRESERVE_IDS=true"]
      }
   }
}
```

The resulting archive keeps every object's id and writes all cross-references by id. Restoring it requires **no special option** — the restore detects the ids and matches accordingly:

- Restoring into the **same** instance is idempotent: objects whose id already exists are skipped instead of being duplicated or merged by name.
- Restoring into a **different** instance preserves the original ids (their ids never collide with the target's, so each object is added keeping its preset id), which lets cross-references between objects and the GWC tile layers re-link correctly.

!!! note
    `BK_PRESERVE_IDS` is currently a REST / programmatic option only; it is not exposed in the Web UI. The default (`false`) keeps the legacy portable, name-based archive format.

## Partial and cross-instance restores

Restoring a workspace-filtered archive, or an archive produced on a different instance, only ever covers part of the target catalog. The extension is resilient to the dangling and missing references that such partial archives carry:

- **Security settings are left untouched when the archive has no security folder.** A workspace-filtered backup (or one taken with `BK_SKIP_SECURITY`) does not include the `security/` directory. In that case the restore detects the missing `security/config.xml` and skips the security step entirely, leaving the target's existing user/group and role services in place. (Previously this scenario could wipe the target security and fail the reload with *"User/group service default does not exist"*.)

- **Dangling style references are dropped instead of corrupting the layer.** If a layer's default or additional style cannot be resolved in the incoming archive, the unresolved reference is dropped (a layer with no explicit default style is still valid) rather than being persisted as an unparseable proxy that would lose the whole layer on the next reload.

- **Stores with an unresolvable workspace are skipped.** A store that references a workspace not present in the target (e.g. a cross-catalog archive) is treated as invalid and skipped, instead of failing the whole store-restore step.

- **Stale GWC tile layers are pruned.** After a (non-dry-run) restore, tile-layer configurations whose published id no longer resolves to a catalog layer or layer group are removed, cleaning up the dangling tile layers that accumulate when a partial restore does not wipe the `gwc-layers` directory.

!!! note
    As noted under [Saving/restoring only specific workspaces](usagegui.md), a backup archive that contains only a subset of workspaces cannot be used to restore the missing ones. To migrate or consolidate a full catalog, back up the whole catalog (optionally with `BK_PRESERVE_IDS=true`) and restore the workspaces you need.
