/* (c) 2016 Open Source Geospatial Foundation - all rights reserved */
package org.geoserver.backuprestore;

import org.geoserver.catalog.Catalog;
import org.springframework.batch.core.JobExecution;

/** Adapter for Restore job executions. */
public class RestoreExecutionAdapter extends AbstractExecutionAdapter {

    private Catalog restoreCatalog;

    /** Legacy constructor (kept for compatibility). */
    public RestoreExecutionAdapter(JobExecution jobExecution, Integer totalNumberOfSteps) {
        super(jobExecution, totalNumberOfSteps);
    }

    public Catalog getRestoreCatalog() {
        return restoreCatalog;
    }

    public void setRestoreCatalog(Catalog restoreCatalog) {
        this.restoreCatalog = restoreCatalog;
    }
}
