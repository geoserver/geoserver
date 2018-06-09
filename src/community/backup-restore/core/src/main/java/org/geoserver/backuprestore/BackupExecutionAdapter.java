/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import org.springframework.batch.core.JobExecution;

/**
 * Wraps a Spring Batch Backup {@link JobExecution} by adding specific {@link Backup} I/O
 * parameters.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class BackupExecutionAdapter extends AbstractExecutionAdapter {

    private boolean overwrite;

    public BackupExecutionAdapter(JobExecution jobExecution, Integer totalNumberOfSteps) {
        super(jobExecution, totalNumberOfSteps);
    }

    /** @return the overwrite */
    public boolean isOverwrite() {
        return overwrite;
    }

    /** @param overwrite the overwrite to set */
    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }
}
