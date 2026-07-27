# Usage via GeoServer's REST API

The Backup and Restore REST API exposes a small set of resources, used asynchronously: you start a backup or restore with a `POST`, then poll the returned execution with a `GET` until it reaches a terminal state. The same engine, options and filters used by the [user interface](usagegui.md) are available here, plus a few REST-only options (see [all options](options.md)).

!!! warning
    Backup and Restore is a GeoServer **community module**. It is experimental and comes with weaker guarantees than official extensions: APIs and behaviour may change between releases. Always run a `BK_DRY_RUN=true` restore first, and keep a known-good archive before restoring into a live instance.

Backup and Restore saves or reloads the GeoServer **catalog and configuration** (workspaces, stores, layers, styles, layer groups, GeoWebCache configuration, services, and global and security settings). It does **not** copy the underlying data — only the configuration that references it.

## Resources

The REST base path is `/rest/br`. Both representations (`.json` and `.xml`) are supported; the examples below use JSON.

| Resource | Method | Description |
|----|----|----|
| `/rest/br/backup` | `GET` | List all backup executions known to the running instance. |
| `/rest/br/backup` | `POST` | Start a new backup. Body is a JSON/XML document with the backup configuration (see below). Returns `201 Created`. |
| `/rest/br/backup/{id}.{json\|xml}` | `GET` | Status of backup execution `{id}`, including per-step progress. |
| `/rest/br/backup/{id}.zip` | `GET` | Download the generated archive for execution `{id}`. |
| `/rest/br/backup/{id}` | `DELETE` | Cancel backup execution `{id}`. Add `?force=true` to escalate a wedged job (see [Cancel or abort a job](#cancel-or-abort-a-job)). |
| `/rest/br/restore` | `GET` | List all restore executions known to the running instance. |
| `/rest/br/restore` | `POST` | Start a new restore from a server-side archive. Body is a JSON/XML document with the restore configuration (see below). Returns `201 Created`. |
| `/rest/br/restore/{id}.{json\|xml}` | `GET` | Status of restore execution `{id}`, including per-step progress. |
| `/rest/br/restore/{id}` | `DELETE` | Cancel restore execution `{id}`. Add `?force=true` to escalate a wedged job. |

!!! note
    The execution list is held **in memory** and is reset when GeoServer restarts. Download archives and note any execution detail you need before restarting the container.

## Request body

A backup or restore is described by a single JSON object whose root key is `backup` or `restore`:

```json
{
  "backup": {
    "archiveFile": "/var/backups/geoserver/backup1.zip",
    "overwrite": true,
    "options": {
      "option": ["BK_SKIP_SECURITY=false", "BK_PARAM_PASSWORDS=true"]
    },
    "wsFilter": "name IN ('topp','sf')"
  }
}
```

| Field | Applies to | Meaning |
|----|----|----|
| `archiveFile` | both | Absolute path, **on the server**, of the `.zip` archive to write (backup) or read (restore). See the note on server-side paths below. |
| `overwrite` | backup | When `true`, replace an existing archive at `archiveFile`. |
| `options` | both | Object with an `option` array of `BK_KEY=value` strings. See [all options](options.md). |
| `wsFilter` / `siFilter` / `liFilter` | both | Optional ECQL strings scoping the job to specific workspaces / stores / layers. See [Filtered backup](#filtered-backup). |

!!! note
    The restore reads the archive from a **server-side path**: `archiveFile` must point to a `.zip` that the GeoServer process can read on its own file system. There is **no** client-side or multipart upload — copy the archive to a location GeoServer can reach, then reference its absolute path. (To move an archive between machines, download it with the `.zip` endpoint and place it on the target server.)

## Examples

The examples use the command-line tool cURL against a GeoServer running at `http://localhost:8080/geoserver`, authenticating as the default `admin` user. Adjust the base URL, credentials and archive paths for your environment.

### Start a full backup

Write the configuration to a file:

`backup_post.json`:

```json
{
  "backup": {
    "archiveFile": "/var/backups/geoserver/backup1.zip",
    "overwrite": true,
    "options": {}
  }
}
```

With no options specified, the defaults apply (see [all options](options.md)). Post it:

```bash
curl -u admin:geoserver -H "Content-Type: application/json" \
  -X POST -d @backup_post.json \
  "http://localhost:8080/geoserver/rest/br/backup"
```

GeoServer answers `201 Created` and returns the new execution. Note its `id` (here, `0`) — you will use it to poll status and download the archive:

```json
{
  "backup": {
    "execution": {
      "id": 0,
      "status": "STARTED",
      "progress": "1/8",
      "archiveFile": "/var/backups/geoserver/backup1.zip",
      "overwrite": true,
      "options": {
        "option": ["OVERWRITE=true"]
      }
    }
  }
}
```

### Poll the status of an execution

Request the execution by id, adding the `.json` (or `.xml`) suffix:

```bash
curl -u admin:geoserver \
  "http://localhost:8080/geoserver/rest/br/backup/0.json"
```

The response reports the overall `status` and `progress` plus the list of steps, each with its own status, exit code and timings. A completed backup looks like:

```json
{
  "backup": {
    "execution": {
      "id": 0,
      "status": "COMPLETED",
      "progress": "8/8",
      "startTime": "2026-06-06T10:21:14.802",
      "endTime": "2026-06-06T10:21:25.356",
      "exitStatus": {
        "exitCode": "COMPLETED",
        "exitDescription": ""
      },
      "stepExecutions": {
        "step": [
          {
            "name": "backupWorkspacesAndLayers",
            "status": "COMPLETED",
            "exitStatus": { "exitCode": "COMPLETED", "exitDescription": "" },
            "readCount": 12,
            "writeCount": 12,
            "failureExceptions": ""
          }
        ]
      },
      "archiveFile": "/var/backups/geoserver/backup1.zip"
    }
  }
}
```

The `status` is one of `STARTING`, `STARTED`, `STOPPING`, `STOPPED`, `COMPLETED`, `FAILED` or `ABANDONED`. Keep polling while it is `STARTING` or `STARTED`. The `failureExceptions` of a failed step carries the message chain and stack trace for troubleshooting.

### Download the archive

Once the backup is `COMPLETED`, download the generated `.zip` by requesting the same resource with a `.zip` suffix:

```bash
curl -u admin:geoserver \
  "http://localhost:8080/geoserver/rest/br/backup/0.zip" -o backup.zip
```

### Start a restore

Restore from a `.zip` already present on the server's file system. Write the configuration:

`restore_post.json`:

```json
{
  "restore": {
    "archiveFile": "/var/backups/geoserver/backup1.zip",
    "options": {}
  }
}
```

Post it:

```bash
curl -u admin:geoserver -H "Content-Type: application/json" \
  -X POST -d @restore_post.json \
  "http://localhost:8080/geoserver/rest/br/restore"
```

As with a backup, GeoServer returns `201 Created` and an execution with an `id`; poll `/rest/br/restore/{id}.json` until it reaches a terminal state.

!!! warning
    With the default options a restore is **destructive**: it purges pre-existing resources (`BK_PURGE_RESOURCES=true`) before restoring. Validate first with a dry run, and pass `BK_PURGE_RESOURCES=false` to merge into the current catalog without deleting. See [all options](options.md).

### Dry-run and fail-on-invalid restore

Validate an archive without changing anything: a dry run assembles the restore catalog and rolls back the data directory when it finishes. Combine it with `BK_FAIL_ON_INVALID=true` to make the validation pass fail the job on the first invalid object instead of only recording warnings:

```json
{
  "restore": {
    "archiveFile": "/var/backups/geoserver/backup1.zip",
    "options": {
      "option": ["BK_DRY_RUN=true", "BK_FAIL_ON_INVALID=true"]
    }
  }
}
```

```bash
curl -u admin:geoserver -H "Content-Type: application/json" \
  -X POST -d @restore_dryrun.json \
  "http://localhost:8080/geoserver/rest/br/restore"
```

A failing dry run ends with `status` `FAILED`; inspect the `failureExceptions` on the offending step. See [Transactional dry-run and fail-on-invalid](migration.md#transactional-dry-run-and-fail-on-invalid).

### Filtered backup

Scope a backup (or restore) to selected workspaces, stores or layers with the `wsFilter`, `siFilter` and `liFilter` ECQL strings. To back up only the `topp` and `sf` workspaces:

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

```bash
curl -u admin:geoserver -H "Content-Type: application/json" \
  -X POST -d @backup_filtered.json \
  "http://localhost:8080/geoserver/rest/br/backup"
```

!!! note
    A workspace-filtered archive omits the global and security configuration, so it cannot be used to restore the workspaces it does **not** contain. To migrate or consolidate a full catalog, back up the whole catalog and restore the workspaces you need. See [the filter reference](options.md#scoping-filters).

### Cross-instance security migration

By default a backup excludes security and a restore does not apply it (`BK_SKIP_SECURITY=true` on both). To carry users, groups and roles to another instance, include security in the backup and **merge** it on restore. Merge mode adds the archive's principals to the target's existing security services, keeping the target's own configuration, keystore and master password — so it works even when the two master passwords differ.

Back up with security included:

```json
{
  "backup": {
    "archiveFile": "/var/backups/geoserver/with-security.zip",
    "overwrite": true,
    "options": {
      "option": ["BK_SKIP_SECURITY=false"]
    }
  }
}
```

Restore it into the target, merging security:

```json
{
  "restore": {
    "archiveFile": "/var/backups/geoserver/with-security.zip",
    "options": {
      "option": ["BK_SKIP_SECURITY=false", "BK_MERGE_SECURITY=true"]
    }
  }
}
```

!!! note
    New principals keep their archived (digest) passwords; reversible passwords must be reset on the target afterwards. To instead **replace** the target's whole security folder from an archive encrypted under a different master password, use the REST-only `BK_SOURCE_MASTER_PASSWORD` / `BK_TARGET_MASTER_PASSWORD` parameters described in [all options](options.md#master-password-parameters).

### Parameterized store passwords

To produce an archive whose store passwords are substitutable tokens instead of encrypted values — useful when the target uses different database credentials — back up with `BK_PARAM_PASSWORDS=true`:

```json
{
  "backup": {
    "archiveFile": "/var/backups/geoserver/parameterized.zip",
    "overwrite": true,
    "options": {
      "option": ["BK_PARAM_PASSWORDS=true"]
    }
  }
}
```

Each store password is written as a token such as `${workspaceName:storeName.passwd.encryptedValue}`. On restore, supply the replacements with `BK_PASSWORD_TOKENS`, a comma-separated list of `token=value` pairs:

```json
{
  "restore": {
    "archiveFile": "/var/backups/geoserver/parameterized.zip",
    "options": {
      "option": [
        "BK_PASSWORD_TOKENS=${ws:store1.passwd.encryptedValue}=secret1,${ws:store2.passwd.encryptedValue}=secret2"
      ]
    }
  }
}
```

!!! tip
    Pass the substituted passwords as transient parameters; never commit them to scripts or logs.

### Cancel or abort a job

Cancel a running backup or restore by sending a `DELETE` with its id:

```bash
curl -u admin:geoserver -X DELETE \
  "http://localhost:8080/geoserver/rest/br/restore/0"
```

The job is asked to stop cooperatively at the next step boundary.

If a job has wedged and is holding the GeoServer configuration write-lock — blocking the rest of the UI and API — an administrator can escalate with `?force=true`. This is a **break-glass, last-resort** action: it interrupts the stuck job and forcibly releases the configuration lock.

```bash
curl -u admin:geoserver -X DELETE \
  "http://localhost:8080/geoserver/rest/br/restore/0?force=true"
```

!!! warning
    Use `?force=true` only when a job is genuinely stuck and the configuration lock will not release on its own. Forcibly releasing the lock mid-write can leave the data directory in an inconsistent state — have a backup ready.

## See also

- [Backup and Restore options](options.md) — the full `BK_*` option reference and the ECQL filters.
- [Usage via the user interface](usagegui.md) — the same operations from the GeoServer web UI.
- [Use cases](usecases.md) — migration, partial restores and ImageMosaic scenarios.
