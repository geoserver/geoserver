/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.listener;

import org.springframework.batch.core.JobExecution;

/** Beans implementing this interface will be invoked as listeners of backup and restore jobs. */
public interface BackupRestoreJobExecutionListener {

    // a job is a backup or a restore job
    enum JobType {
        BACKUP,
        RESTORE
    }

    /** Callback before a job executes. */
    void beforeJob(JobType type, JobExecution jobExecution);

    /**
     * Callback after completion of a job. Called after both both successful and failed executions.
     * To perform logic on a particular status, use "if (jobExecution.getStatus() ==
     * BatchStatus.X)".
     */
    void afterJob(JobType type, JobExecution jobExecution);
}
