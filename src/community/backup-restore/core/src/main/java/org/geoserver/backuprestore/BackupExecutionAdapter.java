/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.util.Set;
import org.springframework.batch.core.job.JobExecution;

/**
 * Wraps a Spring Batch Backup {@link JobExecution} by adding specific {@link Backup} I/O parameters.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class BackupExecutionAdapter extends AbstractExecutionAdapter {

    private boolean overwrite;

    private Set<String> subsetClosure;
    private boolean subsetClosureComputed;

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

    /**
     * @return the workspace-filter dependency-closure ({@link SubsetClosure}) for this backup, computed once and shared
     *     by every step; {@code null} if it could not be computed (the steps then fall back to the plain cascade)
     */
    public Set<String> getSubsetClosure() {
        return subsetClosure;
    }

    /** @return whether {@link #setSubsetClosure(Set)} has run, so the (expensive) closure is computed only once */
    public boolean isSubsetClosureComputed() {
        return subsetClosureComputed;
    }

    /** @param subsetClosure the computed dependency-closure ({@code null} is allowed and marks a failed computation) */
    public void setSubsetClosure(Set<String> subsetClosure) {
        this.subsetClosure = subsetClosure;
        this.subsetClosureComputed = true;
    }
}
