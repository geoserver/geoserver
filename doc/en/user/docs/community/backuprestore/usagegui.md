# Usage Via GeoServer's User Interface {: #backup_restore_usagegui }

At the end on Backup and Restore plugin installation you will see a new section in GeoServer UI

![](images/usagegui001.png)

Clicking on the `Backup and Restore` label will give you access to the Backup and Restore configuration settings:

![](images/usagegui002.png)

Here you'll be able to specify various parameters for the Backup / Restore procedure:

1.  `Archive full path`: Path on the file system to the archive created by the backup procedure, in case a Backup is executed, or the archive to restore from, in case of a Restore procedure.

2.  `Filter by Workspace`: Optional parameter that allows you to restrict the scope of the Backup / Restore to workspaces that meet the specified filter.

3.  `Backup Options`:

    > 1.  `Overwrite Existing Archive`: When enabled the backup procedure will overwrite any previously existing archive
    > 2.  `Skip Failing Resources`: If enabled and errors are found during the backup of existing resources, skip the resource and go ahead with the backup procedure

4.  `Backup Executions`: Report of running and previously run backups

5.  `Restore Options`:

    > 1.  `Dry Run`: Test the restore procedure using the provided archive but do not apply any changes to current configuration. Useful to test archives before actually performing a Restore
    > 2.  `Skip Failing Resources`: If enabled and errors are found during the restore of resources, skip the resource and go ahead with the restore procedure

6.  `Restore Executions`: Report of running and previously run restore

## Performing a full backup via UI

In order to perform a full backup, provide the full path of the target `.zip` archive where to store the configuration data.

!!! note

    Please notice that the backup will store just the configuration files and **not** the original data.

It is also possible to use the `Browse` instrument to navigate the server folders. In any case the backup procedure won't start until it find a valid `.zip` path archive.

![](images/usagegui003.png)

It is possible to select the backup options by enabling the appropriate checkboxes before starting the backup procedure.

!!! note

    Please notice that while performing a backup or restore task, GeoServer won't allow users to access other sections by locking the catalog and configuration until the process has finished. Although it is always possible to stop or abandon a backup or restore procedure.

At the end of the backup, the user will be redirected to an `Execution Summary` page

![](images/usagegui004.png)

The same page can be accessed also later by clicking an execution link from the main page.

!!! note

    Please notice that the list of executions is not persisted and therefore it will be reset after a GeoServer container **restart**.

At the bottom of the `Execution Details` page, it's possible to download the `.zip` archive directly by clicking on the `Download Archive File` link.

![](images/usagegui005.png)

In case some running exceptions or warning have been catched by the process, they will be presented on the execution summary. The `Error Detail Level` allows to inspect the causes by exposing the stack trace for each of them.

## Restoring via UI

The steps are almost the same of the backup. Just select the `.zip` archive full path before launching the restore process.

!!! warning

    Please notice that a **non-dry-run restore** will lose all your current GeoServer configuration by replacing it with the new one, so be careful and be sure to backup everything before starting a restore.

**DRY-RUN RESTORE**

> `Dry Run` option allows a user to **test** a `.zip` archive before actually performing a full restore.
>
> ![](images/usagegui006.png)
>
> ::: note
> ::: title
> Note
> :::
>
> Please notice that the dry run should always being executed when trying to restore a new configuration.
> :::
>
> A **failing** restore dry-run will appear like this
>
> ![](images/usagegui007.png)
>
> If some exception occurs, it will be listed on the execution summary page. The original cause can be inspected by rising up the errors details level and refreshing
>
> ![](images/usagegui008.png)

## Saving/restoring only specific workspaces

It is possible to backup or restore only a subset of the available workspaces in the catalog. From the WEB interface is currently possible to select all or just one workspace to backup/restore

Through the REST APIs it is possible to filter out also more than one workspaces as explained in the next sections.

![](images/usagegui009.png)

!!! note

    Please notice that from a backup archive containing filtered workspaces won't be possible to restore also the missing ones. In order to do that it is advisable to backup the whole catalog and then restore only the workspaces needed.
