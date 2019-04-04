/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import org.geoserver.backuprestore.BackupRestoreItem;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Spring beans implementing this interface will be invoked during backup / restore jobs. Class
 * {@link GenericTaskletUtils} provide utilities methods for implementing this type of handlers.
 */
public interface GenericTaskletHandler {

    /**
     * This method is invoked for each run, which means that this may be invoked multiple times for
     * continuable handlers.
     */
    void initialize(StepExecution stepExecution, BackupRestoreItem context);

    /**
     * This method should do a restore or backup depending on the job context. Class {@link
     * GenericTaskletUtils} provide utilities methods that can be used to retrieve the current type
     * of job and input or output directories.
     */
    RepeatStatus handle(
            StepContribution contribution,
            ChunkContext chunkContext,
            JobExecution jobExecution,
            BackupRestoreItem context);
}
