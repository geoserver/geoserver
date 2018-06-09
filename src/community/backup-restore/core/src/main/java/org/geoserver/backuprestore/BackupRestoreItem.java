/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import com.thoughtworks.xstream.XStream;
import java.util.Arrays;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.util.Assert;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public abstract class BackupRestoreItem<T> {

    /** logger */
    private static final Logger LOGGER = Logging.getLogger(BackupRestoreItem.class);

    protected Backup backupFacade;

    private Catalog catalog;

    protected XStreamPersister xstream;

    private XStream xp;

    private boolean isNew;

    private AbstractExecutionAdapter currentJobExecution;

    private boolean dryRun;

    private boolean bestEffort;

    private XStreamPersisterFactory xStreamPersisterFactory;

    private Filter filter;

    public BackupRestoreItem(Backup backupFacade, XStreamPersisterFactory xStreamPersisterFactory) {
        this.backupFacade = backupFacade;
        this.xStreamPersisterFactory = xStreamPersisterFactory;
    }

    /** @return the xStreamPersisterFactory */
    public XStreamPersisterFactory getxStreamPersisterFactory() {
        return xStreamPersisterFactory;
    }

    /** @return the xp */
    public XStream getXp() {
        return xp;
    }

    /** @param xp the xp to set */
    public void setXp(XStream xp) {
        this.xp = xp;
    }

    /** @return the catalog */
    public Catalog getCatalog() {
        return catalog;
    }

    /** @return the isNew */
    public boolean isNew() {
        return isNew;
    }

    /** @return the currentJobExecution */
    public AbstractExecutionAdapter getCurrentJobExecution() {
        return currentJobExecution;
    }

    /** @return the dryRun */
    public boolean isDryRun() {
        return dryRun;
    }

    /** @return the bestEffort */
    public boolean isBestEffort() {
        return bestEffort;
    }

    /** @return the filter */
    public Filter getFilter() {
        return filter;
    }

    /** @param filter the filter to set */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @BeforeStep
    public void retrieveInterstepData(StepExecution stepExecution) {
        // Accordingly to the running execution type (Backup or Restore) we
        // need to validate resources against the official GeoServer Catalog (Backup)
        // or the temporary one (Restore).
        //
        // For restore operations the order matters.
        JobExecution jobExecution = stepExecution.getJobExecution();
        this.xstream = xStreamPersisterFactory.createXMLPersister();
        if (backupFacade.getRestoreExecutions() != null
                && !backupFacade.getRestoreExecutions().isEmpty()
                && backupFacade.getRestoreExecutions().containsKey(jobExecution.getId())) {
            this.currentJobExecution =
                    backupFacade.getRestoreExecutions().get(jobExecution.getId());
            this.catalog = ((RestoreExecutionAdapter) currentJobExecution).getRestoreCatalog();
            this.isNew = true;
        } else {
            this.currentJobExecution = backupFacade.getBackupExecutions().get(jobExecution.getId());
            this.catalog = backupFacade.getCatalog();
            this.xstream.setExcludeIds();
            this.isNew = false;
        }

        Assert.notNull(this.catalog, "catalog must be set");

        this.xstream.setCatalog(this.catalog);
        this.xstream.setReferenceByName(true);
        this.xp = this.xstream.getXStream();

        Assert.notNull(this.xp, "xStream persister should not be NULL");

        JobParameters jobParameters = this.currentJobExecution.getJobParameters();

        this.dryRun =
                Boolean.parseBoolean(jobParameters.getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        this.bestEffort =
                Boolean.parseBoolean(
                        jobParameters.getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));

        final String cql = jobParameters.getString("filter", null);
        if (cql != null && cql.contains("name")) {
            try {
                this.filter = ECQL.toFilter(cql);
            } catch (CQLException e) {
                throw new IllegalArgumentException("Filter is not valid!", e);
            }
        } else {
            this.filter = null;
        }

        initialize(stepExecution);
    }

    /** */
    protected abstract void initialize(StepExecution stepExecution);

    /**
     * @param result
     * @param e
     * @return
     * @throws Exception
     */
    protected boolean logValidationExceptions(ValidationResult result, Exception e)
            throws Exception {
        CatalogException validationException = new CatalogException(e);
        if (!isBestEffort()) {
            if (result != null) {
                result.throwIfInvalid();
            } else {
                throw e;
            }
        }

        if (!isBestEffort()) {
            getCurrentJobExecution().addFailureExceptions(Arrays.asList(validationException));
        }
        return false;
    }

    /** @param resource */
    protected boolean logValidationExceptions(T resource, Throwable e) {
        CatalogException validationException =
                e != null
                        ? new CatalogException(e)
                        : new CatalogException("Invalid resource: " + resource);
        if (!isBestEffort()) {
            getCurrentJobExecution().addFailureExceptions(Arrays.asList(validationException));
            throw validationException;
        } else {
            getCurrentJobExecution().addWarningExceptions(Arrays.asList(validationException));
        }
        return false;
    }

    /**
     * @param resource
     * @param ws
     * @return
     */
    protected boolean filteredResource(T resource, WorkspaceInfo ws, boolean strict) {
        // Filtering Resources
        if (getFilter() != null) {
            if ((strict && ws == null) || (ws != null && !getFilter().evaluate(ws))) {
                LOGGER.info("Skipped filtered resource: " + resource);
                return true;
            }
        }

        return false;
    }
}
