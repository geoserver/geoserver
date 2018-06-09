/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import org.geoserver.catalog.Catalog;
import org.springframework.batch.core.JobExecution;

/**
 * Wraps a Spring Batch Restore {@link JobExecution} by adding specific {@link Backup} I/O
 * parameters.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class RestoreExecutionAdapter extends AbstractExecutionAdapter {

    private Catalog restoreCatalog;

    public RestoreExecutionAdapter(JobExecution jobExecution, Integer totalNumberOfSteps) {
        super(jobExecution, totalNumberOfSteps);
    }

    /** @return the restoreCatalog */
    public Catalog getRestoreCatalog() {
        return restoreCatalog;
    }

    /** @param restoreCatalog the restoreCatalog to set */
    public void setRestoreCatalog(Catalog catalog) {
        this.restoreCatalog = catalog;
    }
}
