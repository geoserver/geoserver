/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupRestoreItem;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.springframework.batch.core.JobExecution;

/** Utilities methods for generic handlers. */
public final class GenericTaskletUtils {

    private GenericTaskletUtils() {}

    /** Returns TRUE if the current job is a restore job. */
    public static boolean isRestore(BackupRestoreItem context) {
        return context.isNew();
    }

    /** Returns TRUE if the current job is a backup job. */
    public static boolean isBackup(BackupRestoreItem context) {
        return !context.isNew();
    }

    /** Returns TRUE if the current job is a restore job running in a dry mode. */
    public static boolean isDryRun(BackupRestoreItem context) {
        return context.isDryRun();
    }

    /**
     * Can be used in the context of a restore job to get the directory that contain the backup
     * content.
     */
    public static Resource getInputDirectory(JobExecution jobExecution) {
        String inputDirectoryUrl =
                jobExecution.getJobParameters().getString(Backup.PARAM_INPUT_FILE_PATH);
        if (inputDirectoryUrl == null) {
            // this happens if invoked for a backup job
            throw new RuntimeException("No input directory available for this job execution.");
        }
        return Resources.fromURL(inputDirectoryUrl);
    }

    /**
     * Can be used in the context of a backup job to get the directory that will contain the backup
     * content, in the case of a dry run this directory will be removed after the job execution.
     */
    public static Resource getOutputDirectory(JobExecution jobExecution) {
        String outputDirectoryUrl =
                jobExecution.getJobParameters().getString(Backup.PARAM_OUTPUT_FILE_PATH);
        if (outputDirectoryUrl == null) {
            // this happens if invoked for a restore job
            throw new RuntimeException("No output directory available for this job execution.");
        }
        return Resources.fromURL(outputDirectoryUrl);
    }
}
