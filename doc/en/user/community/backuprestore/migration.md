# What changed between 2.x and 3.x

The Backup and Restore module was substantially reworked for GeoServer 3.x. The REST API, the
archive layout and the on-disk format remain compatible, so existing automation and existing
archives keep working. Most of the changes are reliability fixes and new options; there is **one
default behaviour change** — `BK_PRESERVE_IDS` — that you should be aware of before upgrading
scripted backups.

## At a glance

| Area | 2.x | 3.x |
|----|----|----|
| Batch engine | Spring Batch 4 / Spring 5, jobs wired in XML | Spring Batch 6 / Spring 7, jobs wired in Java |
| Default archive | name-based (ids regenerated on restore) | **id-preserving** (`BK_PRESERVE_IDS=true` by default) |
| Filtered (subset) backups | could be incomplete (dangling references) | self-contained via dependency closure |
| Merge / partial restore + GWC | wiped the target's tile layers | keeps the target's tile layers |
| Restore validation | could false-fail name-based restores | null-id guards + trustworthy pre-flight pass |
| Security restore | verbatim copy, single-instance only | merge mode + keystore re-encryption for cross-instance migration |
| Dry-run / fail-on-invalid | wrote to disk incrementally (only the reload was skipped) | snapshot + rollback leave the data directory untouched |
| Cancelling a running job | could return HTTP 500 | clean cancel (UI or `DELETE`); write-lock force-release as break-glass |
| Backup password parameterization | unreliable | `BK_PARAM_PASSWORDS` reliably tokenizes store passwords |

## `BK_PRESERVE_IDS` now defaults to `true` — the one to watch

This is the only change that alters the default outcome of a backup.

In 3.x a backup keeps each catalog object's internal id and writes cross-references **by id**, so the
archive is a portable **migration artifact**: restoring it into another, already-populated GeoServer
preserves the original identities, which lets cross-references and GeoWebCache tile-layer links
re-link correctly, and lets a re-restore into the *same* instance skip objects that already exist
(by id) instead of duplicating or clashing on name.

If you rely on the classic **name-based** archive — ids stripped and regenerated on restore, which is
the right choice when restoring into a fresh, empty data directory — set the option explicitly:

```text
BK_PRESERVE_IDS=false
```

!!! note
    `BK_PRESERVE_IDS` is a **backup-side** option only. The restore adapts automatically to whatever
    the archive contains (it reads whichever of `<id>`/`<name>` each reference carries), so no matching
    option is required on the restore command. See
    [Migrating a catalog to another GeoServer instance](usecases.md#migrating-a-catalog-to-another-geoserver-instance).

## Filtered (subset) backups are now self-contained

A workspace-filtered backup in 2.x captured only the objects directly inside the selected
workspaces. If one of those layers used a **global** (workspace-less) style, or a **layer group**
referenced members in another workspace, the archive carried dangling references and could fail to
restore into an empty target.

In 3.x a filtered backup computes a **dependency closure** before writing: global styles,
cross-workspace layer-group members and the namespaces they need are pulled into the archive
automatically. A subset backup is therefore self-contained and restores cleanly into an empty
instance.

## GeoWebCache tile layers survive a merge restore

In 2.x a restore wiped the target's `gwc-layers` directory whenever the restore was filtered,
destroying tile-layer configuration for layers that had nothing to do with the archive.

In 3.x the GeoWebCache directory is wiped **only on a full purge restore**
(`BK_PURGE_RESOURCES=true` with no workspace filter). A **merge** restore
(`BK_PURGE_RESOURCES=false`) or a **filtered** restore keeps the target's existing tile layers and
merges the archive's tile layers on top. This makes incremental, per-workspace restores safe on a
server that already serves tiles.

## Stronger, trustworthy restore validation

Two fixes make restore validation both less noisy and more dependable:

- **No more false failures on name-based restores** (GEOS-10877). A name-based archive carries
  objects with no ids; in 2.x feeding those to the catalog validator could raise spurious errors.
  In 3.x per-item validation is skipped for null-id objects, so a name-based restore no longer
  false-fails.
- **A pre-flight validation pass.** After the restore catalog has been fully assembled, a dedicated
  step validates the *whole* catalog and reports any problems. By default it logs them and records
  them as execution warnings. Set `BK_FAIL_ON_INVALID=true` to make the restore **abort** on the
  first invalid object — the job is marked `FAILED`, the live configuration reload is skipped, and the
  data directory is rolled back (see [Transactional Dry-Run and fail-on-invalid](#transactional-dry-run-and-fail-on-invalid)
  below). Combine it with `BK_DRY_RUN=true` to validate an archive non-destructively before committing.

## Migration-safe security restore

Restoring security across two different instances used to be impossible: 2.x copied the `security`
folder verbatim, so a keystore encrypted with the source's master password could not be read on a
target with a different one, and the copy overwrote the target's whole security configuration.

3.x adds the pieces needed for cross-instance security migration:

- **`BK_MERGE_SECURITY`** — an *add-only* merge of the archive's users, groups and roles into the
  target's existing security services. The target keeps its own configuration, keystore and master
  password; only principals that are not already present (by name) are added. Works even when the
  source and target master passwords differ.
- **Automatic service bootstrap** — if the target is missing a user/group or role service that the
  merge needs, an empty one is created for it.
- **Keystore re-encryption** — on a security *replace* restore, supply
  `BK_SOURCE_MASTER_PASSWORD` and `BK_TARGET_MASTER_PASSWORD` together and the archive's keystore is
  re-encrypted from the source master password to the target's, so it becomes readable on the target.

See [`BK_MERGE_SECURITY` and the master-password options](usagerest.md) for the REST parameters.

!!! warning
    Security is still **excluded by default** (`BK_SKIP_SECURITY=true`). The options above only take
    effect when you opt security in with `BK_SKIP_SECURITY=false`.

## Transactional Dry-Run and fail-on-invalid

A restore commits to the data directory **incrementally** as its steps run — the restore catalog
persists each item through `GeoServerConfigPersister`, and GeoWebCache and security are written by
their tasklets. In earlier versions a **Dry-Run** therefore only skipped the final in-memory reload:
it still wrote the catalog to disk along the way, so it was not truly non-destructive, and a
`BK_FAIL_ON_INVALID` abort could leave a partial write behind.

3.x makes both opt-in modes trustworthy on disk by snapshotting and rolling back at the job level:

- **Before the job**, when the restore is a Dry-Run (`BK_DRY_RUN=true`) or opts into
  `BK_FAIL_ON_INVALID=true`, the affected data-directory subtrees (`workspaces`, `styles`,
  `layergroups`, `gwc`, `gwc-layers`, `security`) and the root global `*.xml` files are copied into a
  temporary snapshot. This is **strictly opt-in**, so an ordinary restore keeps the historical
  incremental-commit behaviour and pays no snapshot cost.
- **After the job**, if the restore was a Dry-Run or ended in any non-`COMPLETED` state, the live
  tree is rolled back from the snapshot (subtrees the restore created are removed; modified or deleted
  ones are restored) and the configuration and security are reloaded. The snapshot is then deleted; if
  a rollback ever fails it is **preserved** and logged for manual recovery.

The practical effect: a `BK_DRY_RUN=true` restore now leaves the data directory exactly as it was —
making it a safe way to validate an archive — and a failed or `BK_FAIL_ON_INVALID`-aborted restore no
longer leaves the target half-written.

## Cancelling and recovering a running job

A running backup or restore can now be cancelled cleanly, from the web interface or with
`DELETE /rest/br/backup/{id}` / `DELETE /rest/br/restore/{id}`. In 2.x cancelling an in-progress job
could return an HTTP 500; in 3.x the job stops in a well-defined state and the endpoint responds
without error.

If a job becomes wedged while it still holds the GeoServer configuration write-lock — blocking other
configuration changes — an administrator can force the lock to be released:

```text
DELETE /rest/br/restore/{id}?force=true
```

!!! warning
    `force=true` is a **break-glass** mechanism for an administrator. It interrupts the lock owner to
    recover a stuck instance and should be used only when a job is genuinely wedged.

## Reliable backup password parameterization

A backup can replace store passwords with tokens so the archive does not carry plaintext secrets,
re-supplied at restore time. In 3.x `BK_PARAM_PASSWORDS=true` reliably tokenizes store passwords
during backup; a tie-break in converter priority that previously let some passwords slip through
untokenized has been fixed.

## REST API and archive compatibility

The REST endpoints (`/rest/br/backup`, `/rest/br/restore`) and the JSON/XML response shape are
unchanged from 2.x, so existing clients and scripts continue to work without modification. Archives
written by 3.x restore into 3.x; the practical difference from a 2.x archive is the id-preserving
default described above.

## Breaking changes

- **Batch jobs are now defined in Java `@Configuration`.** The custom XML `<batch:job>` and
  `applicationContext.xml` batch definitions from 2.x are gone. Extensions that contributed steps,
  tasklets, readers, processors or writers through XML must be rewired in Java (see *For developers
  and extension authors* below).
- **`BK_PRESERVE_IDS` now defaults to `true`.** Scripted backups that assume name-based,
  id-stripped archives must set `BK_PRESERVE_IDS=false` explicitly (see
  [the section above](#bk_preserve_ids-now-defaults-to-true-the-one-to-watch)).

## For developers and extension authors

- The engine now runs on **Spring Batch 6.0.x / Spring 7** (GeoServer 3's JDK 17 baseline applies).
- The backup and restore **job graphs are defined in Java `@Configuration`**
  (`BackupJobConfiguration`, `RestoreJobConfiguration`, `BatchInfrastructureConfiguration`) instead
  of the old `applicationContext.xml` `<batch:job>` definitions. Custom steps, tasklets, readers,
  processors and writers contributed by extensions should be wired the same way.
