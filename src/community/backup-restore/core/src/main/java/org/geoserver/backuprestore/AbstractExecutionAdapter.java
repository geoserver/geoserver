/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.geoserver.platform.resource.Resource;
import org.geotools.api.filter.Filter;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;

/**
 * Common wrapper for Spring Batch {@link JobExecution}s used by backup/restore.
 *
 * <p>Keeps a reference to {@link JobExplorer} so status/steps are always read fresh, and exposes convenience fields for
 * UI/tests. Provides a simple progress report compatible with the legacy {@code getProgress()} calls (e.g., "4/10
 * (40%)").
 */
public abstract class AbstractExecutionAdapter {

    private volatile JobExecution delegate;
    private JobExplorer jobExplorer;

    private final int totalNumberOfSteps;

    private final List<String> options = new CopyOnWriteArrayList<>();
    private final List<Throwable> failureExceptions = new CopyOnWriteArrayList<>();
    private final List<Throwable> warningExceptions = new CopyOnWriteArrayList<>();

    private Resource archiveFile;
    private Filter wsFilter;
    private Filter siFilter;
    private Filter liFilter;

    protected AbstractExecutionAdapter(JobExecution jobExecution, Integer totalNumberOfSteps) {
        this.delegate = jobExecution;
        this.totalNumberOfSteps = totalNumberOfSteps != null ? totalNumberOfSteps.intValue() : 0;
    }

    /* ----------------------------- Infra / Refresh ----------------------------- */

    /** Inject the JobExplorer so we can fetch fresh state on every read. */
    public void setJobExplorer(JobExplorer jobExplorer) {
        this.jobExplorer = jobExplorer;
    }

    /** Latest JobExecution (refreshed via JobExplorer when available). */
    public JobExecution getDelegate() {
        JobExecution je = this.delegate;
        if (jobExplorer != null && je != null) {
            JobExecution refreshed = jobExplorer.getJobExecution(je.getId());
            if (refreshed != null) {
                this.delegate = je = refreshed;
            }
        }
        return je;
    }

    public long getId() {
        return getDelegate().getId();
    }

    /* ----------------------------- Status / Lifecycle ----------------------------- */

    public BatchStatus getStatus() {
        JobExecution je = getDelegate();
        return je != null ? je.getStatus() : BatchStatus.UNKNOWN;
    }

    /** Conservative running check that does not rely on stale references. */
    public boolean isRunning() {
        BatchStatus s = getStatus();
        return s == BatchStatus.STARTING || s == BatchStatus.STARTED || s == BatchStatus.STOPPING;
    }

    public JobParameters getJobParameters() {
        JobExecution je = getDelegate();
        return je != null ? je.getJobParameters() : null;
    }

    /* ----------------------------- Progress ----------------------------- */

    /** Number of steps that have completed successfully. */
    public int getCompletedSteps() {
        JobExecution je = getDelegate();
        if (je == null) return 0;

        // Prefer step executions from the (refreshed) delegate
        Collection<StepExecution> steps = je.getStepExecutions();
        int completed = 0;
        for (StepExecution se : steps) {
            if (se.getStatus() == BatchStatus.COMPLETED) {
                completed++;
            }
        }
        return completed;
    }

    /** Total number of steps (as provided by the job definition at startup). */
    public int getTotalNumberOfSteps() {
        // fall back to current known steps if not provided
        if (totalNumberOfSteps > 0) return totalNumberOfSteps;
        JobExecution je = getDelegate();
        return je == null ? 0 : new ArrayList<>(je.getStepExecutions()).size();
    }

    /**
     * Legacy-friendly textual progress, e.g. "4/10 (40%)". Safe to call at any time; uses {@link JobExplorer} to avoid
     * stale state.
     */
    public String getProgress() {
        Integer total = this.totalNumberOfSteps;
        if (total == null || total <= 0) return "n/a";
        org.springframework.batch.core.JobExecution je = getDelegate();
        if (je == null) return "n/a";
        long done = je.getStepExecutions().stream()
                .filter(se -> se.getStatus() == org.springframework.batch.core.BatchStatus.COMPLETED)
                .count();
        return done + "/" + total;
    }

    /* ----------------------------- Exceptions ----------------------------- */

    /** Failures recorded by Spring Batch plus adapter-added failures. */
    public List<Throwable> getAllFailureExceptions() {
        List<Throwable> all = new ArrayList<>();
        JobExecution je = getDelegate();
        if (je != null && je.getAllFailureExceptions() != null) {
            all.addAll(je.getAllFailureExceptions());
        }
        all.addAll(this.failureExceptions);
        return all;
    }

    // In AbstractExecutionAdapter
    public void addFailureExceptions(java.util.List<? extends Throwable> ex) {
        if (ex != null) ex.forEach(this::addFailureException);
    }

    public void addWarningExceptions(java.util.List<? extends Throwable> ex) {
        if (ex != null) ex.forEach(this::addWarningException);
    }

    public void addFailureException(Throwable t) {
        if (t != null) this.failureExceptions.add(t);
    }

    public void addFailureExceptions(Collection<? extends Throwable> ts) {
        if (ts != null) this.failureExceptions.addAll(ts);
    }

    /** Non-fatal issues that we want to surface to tests/UI. */
    public List<Throwable> getWarningExceptions() {
        return this.warningExceptions;
    }

    public void addWarningException(Throwable t) {
        if (t != null) this.warningExceptions.add(t);
    }

    public void addWarningExceptions(Collection<? extends Throwable> ts) {
        if (ts != null) this.warningExceptions.addAll(ts);
    }

    /* ----------------------------- Options / I/O ----------------------------- */

    public List<String> getOptions() {
        return options;
    }

    public Resource getArchiveFile() {
        return archiveFile;
    }

    public void setArchiveFile(Resource archiveFile) {
        this.archiveFile = archiveFile;
    }

    public Filter getWsFilter() {
        return wsFilter;
    }

    public void setWsFilter(Filter wsFilter) {
        this.wsFilter = wsFilter;
    }

    public Filter getSiFilter() {
        return siFilter;
    }

    public void setSiFilter(Filter siFilter) {
        this.siFilter = siFilter;
    }

    public Filter getLiFilter() {
        return liFilter;
    }

    public void setLiFilter(Filter liFilter) {
        this.liFilter = liFilter;
    }
}
