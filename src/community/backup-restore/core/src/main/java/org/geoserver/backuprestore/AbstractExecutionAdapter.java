/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.platform.resource.Resource;
import org.opengis.filter.Filter;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

/**
 * Base Class for {@link JobExecution} wrappers. Those will be used to share objects, I/O parameters
 * and GeoServer B/R specific variables and the batch contexts.
 *
 * <p>{@link ConcurrentHashMap}s are populated from the {@link Backup} facade in order to allow
 * external classes to follow jobs executions and retrieve configuration, parameters and statuses.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public abstract class AbstractExecutionAdapter {

    private Integer totalNumberOfSteps;

    private JobExecution delegate;

    private List<String> options = Collections.synchronizedList(new ArrayList<String>());

    private List<Throwable> warningsList = Collections.synchronizedList(new ArrayList<Throwable>());

    private Resource archiveFile;

    private Filter wsFilter;

    private Filter siFilter;

    private Filter liFilter;

    /** Default Constructor */
    public AbstractExecutionAdapter(JobExecution jobExecution, Integer totalNumberOfSteps) {
        this.delegate = jobExecution;
        this.totalNumberOfSteps = totalNumberOfSteps;
    }

    /** @return the delegate */
    public JobExecution getDelegate() {
        return delegate;
    }

    /** @param delegate the delegate to set */
    public void setDelegate(JobExecution delegate) {
        this.delegate = delegate;
    }

    /** The Unique Job Execution ID */
    public Long getId() {
        if (delegate != null) {
            return delegate.getId();
        }
        return null;
    }

    /**
     * Convenience getter for for the id of the enclosing job. Useful for DAO implementations.
     *
     * @return the id of the enclosing job
     */
    public Long getJobId() {
        return delegate.getJobId();
    }

    /**
     * The Spring Batch {@link JobParameters}
     *
     * @return JobParameters of the enclosing job
     */
    public JobParameters getJobParameters() {
        return delegate.getJobParameters();
    }

    /** The Spring Batch Job TimeStamp */
    public Date getTime() {
        return new Date(delegate.getJobParameters().getLong(Backup.PARAM_TIME));
    }

    /**
     * The Spring Batch {@link BatchStatus}
     *
     * <p>ABANDONED COMPLETED FAILED STARTED STARTING STOPPED STOPPING UNKNOWN
     *
     * @return BatchStatus of the enclosing job
     */
    public BatchStatus getStatus() {
        return delegate.getStatus();
    }

    /**
     * The Spring Batch {@link ExitStatus}
     *
     * @return the exitCode of the enclosing job
     */
    public ExitStatus getExitStatus() {
        return delegate.getExitStatus();
    }

    /** Set {@link ExitStatus} of the current Spring Batch Execution */
    public void setExitStatus(ExitStatus exitStatus) {
        delegate.setExitStatus(exitStatus);
    }

    /** Returns all {@link StepExecution}s of the current Spring Batch Execution */
    public Collection<StepExecution> getStepExecutions() {
        return delegate.getStepExecutions();
    }

    /**
     * The Spring Batch {@link JobInstance}
     *
     * @return the Job that is executing.
     */
    public JobInstance getJobInstance() {
        return delegate.getJobInstance();
    }

    /**
     * Test if this {@link JobExecution} indicates that it is running. It should be noted that this
     * does not necessarily mean that it has been persisted as such yet.
     *
     * @return true if the end time is null
     */
    public boolean isRunning() {
        return delegate.isRunning();
    }

    /**
     * Test if this {@link JobExecution} indicates that it has been signalled to stop.
     *
     * @return true if the status is {@link BatchStatus#STOPPING}
     */
    public boolean isStopping() {
        return delegate.isStopping();
    }

    /**
     * Return all failure causing exceptions for this JobExecution, including step executions.
     *
     * @return List&lt;Throwable&gt; containing all exceptions causing failure for this
     *     JobExecution.
     */
    public List<Throwable> getAllFailureExceptions() {
        return delegate.getAllFailureExceptions();
    }

    /**
     * Return all failure marked as warnings by this JobExecution, including step executions.
     *
     * @return List&lt;Throwable&gt; containing all warning exceptions.
     */
    public List<Throwable> getAllWarningExceptions() {
        return warningsList;
    }

    /** Adds exceptions to the current executions marking it as FAILED. */
    public void addFailureExceptions(List<Throwable> exceptions) {
        for (Throwable t : exceptions) {
            this.delegate.addFailureException(t);
        }

        this.delegate.setExitStatus(ExitStatus.FAILED);
    }

    /** Adds exceptions to the current executions as Warnings. */
    public void addWarningExceptions(List<Throwable> exceptions) {
        for (Throwable t : exceptions) {
            this.warningsList.add(t);
        }
    }

    /**
     * Returns the total number of Job steps
     *
     * @return the totalNumberOfSteps
     */
    public Integer getTotalNumberOfSteps() {
        return totalNumberOfSteps;
    }

    /** Returns the current number of executed steps. */
    public Integer getExecutedSteps() {
        return delegate.getStepExecutions().size();
    }

    /** @return the options */
    public List<String> getOptions() {
        return options;
    }

    /** @return */
    public String getProgress() {
        final StringBuffer progress = new StringBuffer();
        progress.append(getExecutedSteps()).append("/").append(getTotalNumberOfSteps());
        return progress.toString();
    }

    /** @return the archiveFile */
    public Resource getArchiveFile() {
        return archiveFile;
    }

    /** @param archiveFile the archiveFile to set */
    public void setArchiveFile(Resource archiveFile) {
        this.archiveFile = archiveFile;
    }

    /** @return the wsFilter */
    public Filter getWsFilter() {
        return wsFilter;
    }

    /** @param wsFilter the wsFilter to set */
    public void setWsFilter(Filter wsFilter) {
        this.wsFilter = wsFilter;
    }

    /** @return the siFilter */
    public Filter getSiFilter() {
        return siFilter;
    }

    /** @param siFilter the siFilter to set */
    public void setSiFilter(Filter siFilter) {
        this.siFilter = siFilter;
    }

    /** @return the liFilter */
    public Filter getLiFilter() {
        return liFilter;
    }

    /** @param liFilter the liFilter to set */
    public void setLiFilter(Filter liFilter) {
        this.liFilter = liFilter;
    }
}
