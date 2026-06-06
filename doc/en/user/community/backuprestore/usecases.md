# Use cases

!!! warning
    Backup and Restore is a community module. It is usable, but does not have the same support guarantees as official extensions.

This page walks through common Backup and Restore scenarios, from a plain full backup to cross-instance migration. Each use case gives a sentence of context and the concrete UI steps or REST snippet to run it. For the full option list and defaults see the [options reference](options.md); for the developer extension points see [the ImageMosaic indexer extension](extensions.md).

Example archive paths below are generic (`/path/to/...`); substitute a real path on your server.

## Full backup and restore

The baseline scenario: archive the whole catalog and configuration, then restore it.

To back up from the UI, open **Backup and Restore**, set **Archive full path** to the target `.zip`, leave the filters empty, and click **Start**. From the REST API:

```json
{
   "backup":{
      "archiveFile":"/path/to/backup/full.zip",
      "overwrite":true,
      "options":{
      }
   }
}
```

To restore, set **Archive full path** to the same `.zip` and click **Start** (run a [dry-run](#validate-before-restore) first). The default restore is destructive — it drops pre-existing resources before restoring — so back up the target first.

!!! note
    The archive holds only the configuration, **not** the underlying data. File- and database-backed stores keep serving from their original locations.

## Migrating a catalog to another GeoServer instance

Since GeoServer 3.0 the default backup archive is **migration-ready**: it keeps every catalog object's id and writes all cross-references by id (`BK_PRESERVE_IDS=true`, the default). This is what you want when the archive will be **merged** into another, already-populated instance — a migration, a consolidation of several servers, or a promotion from staging to production — because it preserves the original identities:

- A restore matches incoming objects **by id** first (then by name), so each object keeps its identity instead of being conflated with a target object that merely shares a name.
- GeoWebCache keys its tile-layer configurations strictly by the **published id** of the layer or layer group (there is no name fallback), so preserving ids is what lets the cached tile layers re-link on the target.

Because this is the default, a migration backup needs no special option:

```json
{
   "backup":{
      "archiveFile":"/path/to/backup/migration.zip",
      "overwrite":true,
      "options":{
      }
   }
}
```

Restoring it requires **no special option** either — the restore detects the ids and matches accordingly:

- Restoring into the **same** instance is idempotent: objects whose id already exists are skipped instead of being duplicated or merged by name.
- Restoring into a **different** instance preserves the original ids (they never collide with the target's, so each object is added keeping its preset id), which lets cross-references between objects and the GWC tile layers re-link correctly.

If you instead want the legacy **portable, name-based** archive — catalog ids stripped and regenerated on restore, the right choice when restoring into a fresh, empty data directory — turn the option off with `BK_PRESERVE_IDS=false`:

```json
{
   "backup":{
      "archiveFile":"/path/to/backup/portable.zip",
      "overwrite":true,
      "options":{
        "option": ["BK_PRESERVE_IDS=false"]
      }
   }
}
```

!!! note
    `BK_PRESERVE_IDS` is exposed in the web UI as the **Preserve Catalog IDs (for migration to another instance)** backup checkbox (checked by default). It is a backup-side option only; the restore adapts automatically to whatever the archive contains. See [what changed between 2.x and 3.x](migration.md).

## Partial and cross-instance restores

Restoring a filtered archive, or an archive produced on a different instance, only ever covers part of the target catalog. Filter by **Workspace**, **Store** or **Layer** (ECQL) to scope the job. The extension is resilient to the dangling and missing references that such partial archives carry:

- **Security settings are left untouched when the archive has no security folder.** A workspace-filtered backup (or one taken with `BK_SKIP_SECURITY`) does not include the `security/` directory. In that case the restore detects the missing `security/config.xml` and skips the security step entirely, leaving the target's existing user/group and role services in place.
- **Dangling style references are dropped instead of corrupting the layer.** If a layer's default or additional style cannot be resolved in the incoming archive, the unresolved reference is dropped (a layer with no explicit default style is still valid) rather than being persisted as an unparseable proxy that would lose the whole layer on the next reload.
- **Stores with an unresolvable workspace are skipped.** A store that references a workspace not present in the target (for example, a cross-catalog archive) is treated as invalid and skipped, instead of failing the whole store-restore step.
- **Stale GWC tile layers are pruned.** After a (non-dry-run) restore, tile-layer configurations whose published id no longer resolves to a catalog layer or layer group are removed, cleaning up the dangling tile layers that accumulate when a partial restore does not wipe the `gwc-layers` directory.

To merge a partial archive into an existing catalog without deleting anything, restore with **Purge Existing Resources** unchecked (`BK_PURGE_RESOURCES=false`).

!!! note
    A backup archive that contains only a subset of workspaces cannot be used to restore the missing ones. To migrate or consolidate a full catalog, back up the whole catalog (the default archive already preserves ids for migration) and restore the workspaces you need.

## Cross-instance security migration

Restoring the security configuration verbatim across two instances fails when their master passwords differ: a keystore encrypted with the source's master password cannot be read on the target, and a verbatim copy overwrites the target's whole security configuration. The 3.x module offers two ways to migrate security across instances. Both require opting security in (uncheck **Skip Security Settings**, or `BK_SKIP_SECURITY=false`), since security is excluded by default.

**Merge mode** adds only the principals the target is missing. **Merge Security** (`BK_MERGE_SECURITY=true`) performs an add-only merge of the archive's users, groups and roles into the target's existing security services; the target keeps its own configuration, keystore and master password, and a missing user/group or role service the merge needs is bootstrapped empty. New users keep their archived (digest) passwords; reversible passwords must be reset afterwards.

```json
{
   "restore":{
      "archiveFile":"/path/to/backup/source-instance.zip",
      "options":{
        "option": ["BK_SKIP_SECURITY=false", "BK_MERGE_SECURITY=true"]
      }
   }
}
```

**Replace mode with keystore re-encryption** brings the source's security configuration across wholesale and makes its keystore readable on the target. Supply both `BK_SOURCE_MASTER_PASSWORD` and `BK_TARGET_MASTER_PASSWORD` so the archive's keystore is re-encrypted from the source master password to the target's:

```json
{
   "restore":{
      "archiveFile":"/path/to/backup/source-instance.zip",
      "options":{
        "option": [
          "BK_SKIP_SECURITY=false",
          "BK_SOURCE_MASTER_PASSWORD=sourceMasterPwd",
          "BK_TARGET_MASTER_PASSWORD=targetMasterPwd"
        ]
      }
   }
}
```

!!! note
    `BK_MERGE_SECURITY` is exposed in the UI as the **Merge Security (cross-instance migration)** restore checkbox. The master-password options are REST-only. See the [options reference](options.md) and [migration-safe security restore](migration.md#migration-safe-security-restore).

## Parameterized store passwords

To avoid carrying encrypted store passwords in the archive — for example, when the same archive is restored into environments with different credentials — parameterize them on backup and substitute concrete values on restore.

On **backup**, enable **Parameterize Store Passwords** (`BK_PARAM_PASSWORDS=true`). Each store password is written as a token of the form `${workspaceName:storeName.passwd.encryptedValue}` instead of an encrypted value:

```json
{
   "backup":{
      "archiveFile":"/path/to/backup/parameterized.zip",
      "overwrite":true,
      "options":{
        "option": ["BK_PARAM_PASSWORDS=true"]
      }
   }
}
```

On **restore**, pass `BK_PASSWORD_TOKENS` — a comma-separated list of `token=value` pairs — to substitute each token with the concrete password for the target environment:

```json
{
   "restore":{
      "archiveFile":"/path/to/backup/parameterized.zip",
      "options":{
        "option": ["BK_PASSWORD_TOKENS=${workspace:store1.passwd.encryptedValue}=secret1,${workspace:store2.passwd.encryptedValue}=secret2"]
      }
   }
}
```

!!! note
    `BK_PARAM_PASSWORDS` is exposed in the UI as the **Parameterize Store Passwords** backup checkbox. `BK_PASSWORD_TOKENS` is REST-only. See [Usage via the REST API](usagerest.md).

## Validate before restore

Validate an archive non-destructively before committing it. A **dry-run** (the **Dry-Run** checkbox, or `BK_DRY_RUN=true`) runs the whole restore but snapshots and rolls back the affected data-directory subtrees, leaving the data directory untouched. Combine it with **Fail-on-Invalid** (`BK_FAIL_ON_INVALID=true`) to make the pre-flight validation pass **abort** on the first invalid object instead of only logging warnings:

```json
{
   "restore":{
      "archiveFile":"/path/to/backup/full.zip",
      "options":{
        "option": ["BK_DRY_RUN=true", "BK_FAIL_ON_INVALID=true"]
      }
   }
}
```

!!! note
    `BK_DRY_RUN` is exposed in the UI as the **Dry-Run** restore checkbox; `BK_FAIL_ON_INVALID` is REST-only. See [transactional dry-run and fail-on-invalid](migration.md#transactional-dry-run-and-fail-on-invalid).

## ImageMosaic indexer parameterization

The remaining use cases cover ImageMosaic coverage stores, whose `.properties` files instruct the reader on how to build the mosaic index. Backup and Restore can **inject environment properties** into these indexer files so a mosaic can be ported between environments. This builds on the [ImageMosaic indexer extension](extensions.md) points.

### Database- vs. shapefile-backed indexer

When a mosaic uses a database as the index backend, a `datastore.properties` file in the mosaic folder holds the connection parameters. To parameterize it, create a `.template` datastore-properties file containing the same properties but with placeholders as values:

```text
host=${mosaic1.jdbc.host}
port=${mosaic1.jdbc.port}
...
```

The extension saves both the original `.properties` and the `.template` to the archive. On restore it overwrites the `.properties` from the `.template`, substituting the placeholders with the matching environment property values.

When a shapefile is the index backend, the shapefile itself is recreated by the mosaic on its first harvest, so it does not need to be archived.

### Database connection parameters vs. JNDI

The same as the previous case, except a parametric JNDI name replaces the host/port parameters.

### Indexer files and regex

The approach is identical to `datastore.properties`. Note that the extension overwrites only the files that have a corresponding `.template` prototype.

### Granules in the mosaic folder vs. absolute path

This does not affect Backup and Restore, since it never dumps data into the archive. Absolute granule paths should be parameterized in the same way as the connection parameters above.

### Non-existing index on the target environment

When restoring an ImageMosaic, the index may not exist on the target. After restoring `datastore.properties`, the extension checks the index store:

1. If it cannot connect to the datastore, the resource fails.
2. If the datastore is reachable but the index does not exist, the extension creates an empty mosaic in the catalog instead of failing.
