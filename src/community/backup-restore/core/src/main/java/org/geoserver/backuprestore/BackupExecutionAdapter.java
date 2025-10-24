/* (c) 2016 Open Source Geospatial Foundation - all rights reserved */
package org.geoserver.backuprestore;

import org.springframework.batch.core.JobExecution;

/** Adapter for Backup job executions. */
public class BackupExecutionAdapter extends AbstractExecutionAdapter {

    private boolean overwrite;

    /** Legacy constructor (kept for compatibility). */
    public BackupExecutionAdapter(JobExecution jobExecution, Integer totalNumberOfSteps) {
        super(jobExecution, totalNumberOfSteps);
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }
}
