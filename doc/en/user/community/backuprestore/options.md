# Backup and Restore options

This page is the authoritative reference for the `BK_*` options that control a Backup and Restore job, the ECQL scoping filters, and the master-password parameters. The same options are used from the [user interface](usagegui.md) (as checkboxes) and the [REST API](usagerest.md) (as `BK_KEY=value` strings).

!!! warning
    Backup and Restore is a GeoServer **community module**. It is experimental and comes with weaker guarantees than official extensions: option names, defaults and behaviour may change between releases. Always run a `BK_DRY_RUN=true` restore first.

Backup and Restore saves or reloads the GeoServer **catalog and configuration** (workspaces, stores, layers, styles, layer groups, GeoWebCache configuration, services, and global and security settings). It does **not** copy the underlying data — only the configuration that references it. The options below decide *which* parts of that configuration are written or read, and how the restore reconciles them with the target.

## Option reference

In REST requests, each option is a `BK_KEY=value` string in the `options.option` array (see [Usage via REST](usagerest.md)). In the UI, the options marked with a GUI label are checkboxes on the Backup or Restore panel; the rest are REST-only.

| Key | Applies to | Meaning | Default | GUI checkbox |
|----|----|----|----|----|
| `BK_BEST_EFFORT` | both | Keep going when a resource fails instead of aborting the whole job; the failure is recorded as a warning in the execution summary. | `false` | Skip Failing Resources |
| `BK_DRY_RUN` | restore | Validate and report what the restore would do without persisting changes; the affected data-directory subtrees are snapshotted and rolled back. | `false` | Dry-Run |
| `BK_SKIP_SETTINGS` | both | Exclude the global settings (contact info, JAI, service defaults, ...) from the backup / restore. | `true` | Skip Global Settings |
| `BK_SKIP_SECURITY` | both | Exclude the security configuration (users, groups, roles, authentication) from the backup / restore. A workspace filter also forces security to be skipped. | `true` | Skip Security Settings |
| `BK_PURGE_RESOURCES` | restore | Delete pre-existing resources (e.g. drop existing workspaces) before restoring. Set to `false` to **merge** the archive into the current catalog without deleting anything. | `true` | Purge Existing Resources |
| `BK_SKIP_GWC` | both | Exclude the GeoWebCache configuration and tile-layer folders from the backup / restore. | `false` | Skip GeoWebCache |
| `BK_MERGE_SECURITY` | restore | Merge the archive's users, groups and roles into the target's existing security services instead of replacing the whole `security` folder, keeping the target's configuration, keystore and master password. Has effect only when security is actually restored (`BK_SKIP_SECURITY=false`). | `false` | Merge Security (cross-instance migration) |
| `BK_PARAM_PASSWORDS` | backup | Write store passwords as substitutable `${...}` tokens instead of the encrypted values, so the archive can be restored into an environment that uses different credentials. Supply the replacements with `BK_PASSWORD_TOKENS` on restore. | `false` | Parameterize Store Passwords |
| `BK_PASSWORD_TOKENS` | restore | Substitution map for a parameterized archive: a comma-separated list of `token=value` pairs replacing the `${...}` store-password tokens. | *(none)* | REST-only |
| `BK_PRESERVE_IDS` | backup | Keep every catalog object's internal id and write cross-references **by id**, producing a portable migration archive that restores into another instance with the original identities — and their GWC tile-layer links — preserved. Set to `false` for the legacy name-based archive whose ids are stripped and regenerated on restore. | `true` | Preserve Catalog IDs (for migration to another instance) |
| `BK_FAIL_ON_INVALID` | restore | Run a pre-flight validation pass over the assembled restore catalog and abort (job `FAILED`, live reload skipped, data directory rolled back) if any object is invalid. When `false`, the pass still runs but only logs problems and records them as warnings. | `false` | REST-only |
| `BK_CLEANUP_TEMP` | both | Delete the temporary working folder when the job finishes. Turn off only to inspect the intermediate files while troubleshooting. | `false` | Clean-Up Temp Resources |
| `BK_SOURCE_MASTER_PASSWORD` | restore | The source instance's master password, supplied with `BK_TARGET_MASTER_PASSWORD` on a security **replace** restore so the archive's keystore can be re-encrypted to the target's master password. | *(none)* | REST-only |
| `BK_TARGET_MASTER_PASSWORD` | restore | The target instance's master password, required alongside `BK_SOURCE_MASTER_PASSWORD` for the keystore re-encryption. Must match the target's actual master password or the re-encryption is rejected. | *(none)* | REST-only |
| `exclude.file.path` | both | A `;`-separated list of paths, relative to `GEOSERVER_DATA_DIR`, to skip (e.g. `/data/geonode;/monitoring`). For custom external resources under the data directory only — `security` and `workspaces` are handled separately. | *(empty)* | REST-only |

!!! note
    The default-`true` options — `BK_SKIP_SETTINGS`, `BK_SKIP_SECURITY`, `BK_PURGE_RESOURCES` and `BK_PRESERVE_IDS` — must be passed an explicit `false` to disable them. For example, a backup **excludes** security unless you send `BK_SKIP_SECURITY=false`, and a restore **purges** existing resources unless you send `BK_PURGE_RESOURCES=false`. The UI exposes these as checkboxes pre-checked to the same defaults.

## Scoping filters

Three optional ECQL strings restrict a job to part of the catalog. They are top-level fields of the request body in REST (`wsFilter` / `siFilter` / `liFilter`), and the **Filter by Workspace / Store / Layer** dropdowns in the UI.

| Field | Scopes to | Matches against |
|----|----|----|
| `wsFilter` | Workspace(s) | workspace properties (e.g. `name`) |
| `siFilter` | Store(s) | store properties (e.g. `name`) |
| `liFilter` | Layer(s) | layer/resource properties (e.g. `name`) |

For example, to scope a job to the `topp` and `sf` workspaces:

```text
name IN ('topp','sf')
```

In a REST request body:

```json
{
  "backup": {
    "archiveFile": "/var/backups/geoserver/subset.zip",
    "overwrite": true,
    "options": {},
    "wsFilter": "name IN ('topp','sf')"
  }
}
```

!!! note
    A workspace filter forces security to be skipped, and a filtered archive omits the global and security configuration. Such a subset archive cannot restore the workspaces it does not contain; back up the whole catalog to migrate or consolidate. See [Partial and cross-instance restores](usecases.md#partial-and-cross-instance-restores).

## Master-password parameters

`BK_SOURCE_MASTER_PASSWORD` and `BK_TARGET_MASTER_PASSWORD` apply only to a security **replace** restore — that is, `BK_SKIP_SECURITY=false` **and** `BK_MERGE_SECURITY=false`. When the archive's keystore was encrypted under a master password different from the target's, supply both so the keystore can be re-encrypted from the source master password to the target's. Without them, a keystore encrypted under a different source master password cannot be read on the target.

`BK_TARGET_MASTER_PASSWORD` must match the target instance's actual master password or the re-encryption is rejected. Both are **sensitive**: pass them as transient parameters and never store them in scripts or logs.

For cross-instance security migration where the two master passwords differ, prefer **merge mode** (`BK_MERGE_SECURITY=true`) instead — it keeps the target's own keystore and master password and needs no master-password parameters. See [cross-instance security migration](usagerest.md#cross-instance-security-migration).

## See also

- [Usage via the user interface](usagegui.md) — the options as UI checkboxes.
- [Usage via REST](usagerest.md) — worked cURL examples that combine these options.
- [Use cases](usecases.md) — migration, partial restores and ImageMosaic scenarios.
