# Usage via the web interface

!!! warning
    Backup and Restore is a community module. It is usable, but does not have the same support guarantees as official extensions.

Once the Backup and Restore plugin is installed, a new **Backup and Restore** section appears in the GeoServer web admin interface.

![GeoServer admin menu showing the new Backup and Restore section](images/usagegui001.png)

Clicking the **Backup and Restore** link opens the configuration page, where you set the archive path, optional filters and the backup or restore options.

![Backup and Restore configuration page with archive path, filter and option checkboxes](images/usagegui002.png)

## Configuration parameters

- **Archive full path** — path on the server file system to the archive to create (backup) or to read (restore). The job does not start until a valid `.zip` path is given. Use **Browse** to navigate the server folders.
- **Filter by Workspace / Store / Layer** — optional filters that restrict the job to the matching catalog objects. See [Saving or restoring only specific workspaces](#saving-or-restoring-only-specific-workspaces) below.
- **Backup options** and **Restore options** — the checkboxes summarised below.
- **Backup Executions** and **Restore Executions** — reports of running and previously run jobs.

### Backup options

The backup checkboxes select what the archive contains and how it is written. Common ones:

- **Overwrite Existing Archive** — replace an archive that already exists at the target path.
- **Skip Failing Resources** — best-effort backup: log and skip a resource that fails instead of aborting the job.
- **Clean-Up Temp Resources** — delete the temporary working folder when the job ends.
- **Skip GeoWebCache** — exclude the GWC catalog and tile-layer folders.
- **Parameterize Store Passwords** — write store passwords as parameterizable tokens instead of encrypted values (the REST `BK_PARAM_PASSWORDS` option).
- **Preserve Catalog IDs (for migration to another instance)** — keep catalog object ids and write cross-references by id, producing a portable migration archive. **On by default.** Turn it off only to produce a legacy name-based archive whose ids are regenerated on restore. See [Migrating a catalog to another GeoServer instance](usecases.md#migrating-a-catalog-to-another-geoserver-instance).
- **Skip Security Settings** — exclude the security configuration (users, groups, roles, services). **On by default.**
- **Skip Global Settings** — exclude the global settings. **On by default.**

For the complete checkbox list, defaults and the matching REST option names, see the [options reference](options.md).

### Restore options

The restore checkboxes select what is restored and whether the target catalog is purged first. Common ones:

- **Dry-Run** — validate the archive without applying any change to the running configuration. See [Dry-run restore](#dry-run-restore).
- **Skip Failing Resources** — best-effort restore: log and skip a resource that fails instead of aborting the job.
- **Clean-Up Temp Resources** — delete the temporary working folder when the job ends.
- **Skip GeoWebCache** — exclude the GWC catalog and tile-layer folders.
- **Skip Security Settings** — do not restore the security configuration. **On by default.**
- **Skip Global Settings** — do not restore the global settings. **On by default.**
- **Purge Existing Resources** — drop incoming resources where possible before restoring (for example, remove existing workspaces). **On by default.** Uncheck it to merge into the existing catalog without deleting.
- **Merge Security (cross-instance migration)** — add the archive's users, groups and roles to this instance's existing security services instead of replacing the whole security configuration. **Off by default** (replace mode). Applies only when security is actually restored — that is, when **Skip Security Settings** is unchecked.

For the complete checkbox list, defaults and the matching REST option names, see the [options reference](options.md).

!!! warning
    **Skip Security Settings** is checked by default for a reason: restoring security configuration replaces the target's users, groups, roles and authentication settings. Uncheck it only when you understand the archive's security content and intend to overwrite the target. See the [cross-instance security migration](usecases.md#cross-instance-security-migration) use case.

!!! note
    Some options — password-token substitution, the pre-flight validation gate (`BK_FAIL_ON_INVALID`) and security keystore re-encryption (`BK_SOURCE_MASTER_PASSWORD` / `BK_TARGET_MASTER_PASSWORD`) — are available only through the [REST API](usagerest.md), which exposes the full option set. See the [options reference](options.md).

## Performing a full backup

To perform a full backup, provide the full path of the target `.zip` archive where the configuration is stored, then select any backup options before starting.

!!! note
    The backup stores only the configuration files, **not** the original data.

![Backup configuration filled in with a target archive path ready to start](images/usagegui003.png)

!!! note
    While a backup or restore runs, GeoServer locks the catalog and configuration, so other sections are unavailable until the job finishes. You can still stop or abandon a running job — see [Cancelling a running job](#cancelling-a-running-job).

While the job runs, the status next to the **Start** button updates automatically and reports progress as `<status> — step <done>/<total> (<current step>)`. When the backup finishes, you are redirected to an **Execution Summary** page.

![Backup execution summary page reporting a completed job](images/usagegui004.png)

The same page can be reached later by clicking an execution link on the main page.

!!! note
    The **Execution Details** page refreshes itself while the job runs, so step states and progress update without any action; a manual **refresh** link is also available. Auto-refresh stops once the job reaches a terminal state.

!!! note
    The list of executions is not persisted, so it is reset after a GeoServer container **restart**.

At the bottom of the **Execution Details** page, the **Download Archive File** link downloads the `.zip` archive directly.

![Execution details page with the Download Archive File link at the bottom](images/usagegui005.png)

If the job caught any exceptions or warnings, they are listed on the execution summary. The **Error Detail Level** control reveals the stack trace for each, so you can inspect the cause.

## Restoring

The steps mirror a backup: select the `.zip` archive full path and any restore options, then start the restore.

!!! warning
    A **non-dry-run restore** replaces your current GeoServer configuration with the archive's contents. Back up everything before starting a restore.

### Dry-run restore

The **Dry-Run** option lets you **test** a `.zip` archive before performing a full restore. Since 3.x a dry-run snapshots and rolls back the affected data-directory subtrees, so it leaves the data directory untouched.

![Restore configuration with the Dry-Run option enabled](images/usagegui006.png)

!!! note
    Always run a dry-run before restoring a new configuration.

A **failing** dry-run is reported on the execution summary.

![Execution summary of a failing dry-run restore](images/usagegui007.png)

Raise the **Error Detail Level** and refresh to expose the original cause of each failure.

![Execution details with the error detail level raised to show the stack trace](images/usagegui008.png)

## Cancelling a running job

A backup or restore can be stopped while it is still running. From the **Backup Executions** or **Restore Executions** report, open the running execution and use the stop control to **stop** (graceful) or **abandon** (force) it; the job moves to a terminal state and the configuration lock is released. The same can be done from automation with `DELETE /rest/br/backup/{id}` or `DELETE /rest/br/restore/{id}` — see [Usage via the REST API](usagerest.md).

## Saving or restoring only specific workspaces

You can back up or restore a subset of the catalog. From the web interface you can select all workspaces or a single workspace to back up or restore. The REST API additionally lets you filter more than one workspace, and filter by store or layer with ECQL — see [Usage via the REST API](usagerest.md).

![Filter by Workspace control selecting a single workspace for a partial backup](images/usagegui009.png)

!!! note
    An archive that contains only a subset of workspaces cannot be used to restore the missing ones. To migrate or consolidate a full catalog, back up the whole catalog (the default archive already preserves ids for migration) and restore only the workspaces you need.
