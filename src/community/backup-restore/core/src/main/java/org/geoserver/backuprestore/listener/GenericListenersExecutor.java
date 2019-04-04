/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.listener;

import java.util.List;
import org.geoserver.backuprestore.listener.BackupRestoreJobExecutionListener.JobType;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/** Job execution listener that will invoke listeners contributed by extensions. */
public final class GenericListenersExecutor implements JobExecutionListener {

    // type of the job associated to this listener instance (backup or restore)
    private final JobType jobType;

    public GenericListenersExecutor(boolean backup) {
        jobType = backup ? JobType.BACKUP : JobType.RESTORE;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        getListeners().forEach(listener -> listener.beforeJob(jobType, jobExecution));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        getListeners().forEach(listener -> listener.afterJob(jobType, jobExecution));
    }

    /** Helper method that returns all the available job execution listeners. */
    private List<BackupRestoreJobExecutionListener> getListeners() {
        return GeoServerExtensions.extensions(BackupRestoreJobExecutionListener.class);
    }
}
