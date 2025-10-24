/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Primary controller/facade of the backup and restore subsystem.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Starts Backup/Restore Spring Batch jobs with parameters
 *   <li>Tracks running executions with {@link JobExplorer}
 *   <li>Coordinates begin/end request callbacks (catalog locking)
 *   <li>Provides helper utilities for XStream (XML/JSON) serialization
 * </ul>
 *
 * <p><b>Spring Batch 5.x note:</b> Jobs launched asynchronously must be polled via {@link JobExplorer} for fresh state.
 */
@SuppressWarnings("rawtypes")
public class Backup
        implements DisposableBean,
                ApplicationContextAware,
                ApplicationListener<ApplicationEvent>,
                JobExecutionListener {

    /* ====== Job parameter keys ====== */
    public static final String PARAM_PASSWORD_TOKENS = "BK_PASSWORD_TOKENS";
    public static final String PARAM_SKIP_SETTINGS = "BK_SKIP_SETTINGS";
    public static final String PARAM_SKIP_SECURITY_SETTINGS = "BK_SKIP_SECURITY";
    public static final String PARAM_PURGE_RESOURCES = "BK_PURGE_RESOURCES";
    public static final String PARAM_SKIP_GWC = "BK_SKIP_GWC";
    public static final String PARAM_TIME = "time";
    public static final String PARAM_JOB_NAME = "job.execution.name";
    public static final String PARAM_OUTPUT_FILE_PATH = "output.file.path";
    public static final String PARAM_INPUT_FILE_PATH = "input.file.path";
    public static final String PARAM_EXCLUDE_FILE_PATH = "exclude.file.path";
    public static final String PARAM_CLEANUP_TEMP = "BK_CLEANUP_TEMP";
    public static final String PARAM_DRY_RUN_MODE = "BK_DRY_RUN";
    public static final String PARAM_BEST_EFFORT_MODE = "BK_BEST_EFFORT";
    public static final String PARAM_PARAMETERIZE_PASSWDS = "BK_PARAM_PASSWORDS";

    /* ====== Job & context keys ====== */
    public static final String BACKUP_JOB_NAME = "backupJob";
    public static final String RESTORE_JOB_NAME = "restoreJob";
    public static final String RESTORE_CATALOG_KEY = "restore.catalog";

    private static final Logger LOGGER = Logging.getLogger(Backup.class);

    private Authentication auth;

    private final Catalog catalog;
    private final GeoServer geoServer;
    private GeoServerResourceLoader resourceLoader;
    private GeoServerDataDirectory geoServerDataDirectory;
    private final XStreamPersisterFactory xpf;

    private JobOperator jobOperator;
    private JobLauncher jobLauncher;
    private JobRepository jobRepository;
    private JobExplorer jobExplorer;

    private Job backupJob;
    private Job restoreJob;

    private final ConcurrentHashMap<Long, BackupExecutionAdapter> backupExecutions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, RestoreExecutionAdapter> restoreExecutions = new ConcurrentHashMap<>();

    private Integer totalNumberOfBackupSteps;
    private Integer totalNumberOfRestoreSteps;

    /** Static Spring context kept for compatibility with legacy lookups. */
    private static ApplicationContext context;

    public Backup(final Catalog catalog, final GeoServerResourceLoader rl) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
        this.resourceLoader = Objects.requireNonNull(rl, "resourceLoader must not be null");
        this.geoServerDataDirectory = new GeoServerDataDirectory(rl);
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public JobOperator getJobOperator() {
        return jobOperator;
    }

    public JobLauncher getJobLauncher() {
        return jobLauncher;
    }

    public JobRepository getJobRepository() {
        return jobRepository;
    }

    public JobExplorer getJobExplorer() {
        return jobExplorer;
    }

    public Job getBackupJob() {
        return backupJob;
    }

    public Job getRestoreJob() {
        return restoreJob;
    }

    public ConcurrentHashMap<Long, BackupExecutionAdapter> getBackupExecutions() {
        return backupExecutions;
    }

    public ConcurrentHashMap<Long, RestoreExecutionAdapter> getRestoreExecutions() {
        return restoreExecutions;
    }

    public Authentication getAuth() {
        return auth;
    }

    public void setAuth(Authentication auth) {
        this.auth = auth;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public GeoServer getGeoServer() {
        return geoServer;
    }

    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader);
    }

    public GeoServerDataDirectory getGeoServerDataDirectory() {
        return geoServerDataDirectory;
    }

    public void setGeoServerDataDirectory(GeoServerDataDirectory geoServerDataDirectory) {
        this.geoServerDataDirectory = Objects.requireNonNull(geoServerDataDirectory);
    }

    public Integer getTotalNumberOfBackupSteps() {
        return totalNumberOfBackupSteps;
    }

    public void setTotalNumberOfBackupSteps(Integer v) {
        this.totalNumberOfBackupSteps = v;
    }

    public Integer getTotalNumberOfRestoreSteps() {
        return totalNumberOfRestoreSteps;
    }

    public void setTotalNumberOfRestoreSteps(Integer v) {
        this.totalNumberOfRestoreSteps = v;
    }

    /* =========================================================
    Spring lifecycle hooks
    ========================================================= */

    @Override
    public void destroy() {
        // nothing to do
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Backup.context = ctx;

        try {
            AbstractJob bj = (AbstractJob) ctx.getBean(BACKUP_JOB_NAME);
            if (bj != null) setTotalNumberOfBackupSteps(bj.getStepNames().size());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not determine backup steps", e);
        }

        try {
            // FIX: use RESTORE_JOB_NAME (was BACKUP_JOB_NAME twice before)
            AbstractJob rj = (AbstractJob) ctx.getBean(RESTORE_JOB_NAME);
            if (rj != null) setTotalNumberOfRestoreSteps(rj.getStepNames().size());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not determine restore steps", e);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextLoadedEvent) {
            this.jobOperator = getBean("jobOperator", JobOperator.class);
            this.jobLauncher = getBean("jobLauncherAsync", JobLauncher.class);
            this.jobRepository = getBean("jobRepository", JobRepository.class);
            this.jobExplorer = getBean("jobExplorer", JobExplorer.class);
            this.backupJob = getBean(BACKUP_JOB_NAME, Job.class);
            this.restoreJob = getBean(RESTORE_JOB_NAME, Job.class);

            // Ensure we get lifecycle callbacks even if not wired in XML
            if (backupJob instanceof AbstractJob bj) bj.registerJobExecutionListener(this);
            if (restoreJob instanceof AbstractJob rj) rj.registerJobExecutionListener(this);

            LOGGER.info(() -> "Backup facade initialized: "
                    + "jobLauncher=" + (jobLauncher != null)
                    + ", jobRepository=" + (jobRepository != null)
                    + ", jobExplorer=" + (jobExplorer != null)
                    + ", jobs=[" + BACKUP_JOB_NAME + "," + RESTORE_JOB_NAME + "]");
        }
    }

    private static <T> T getBean(String name, Class<T> type) {
        try {
            return type.cast(context.getBean(name));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to resolve bean '" + name + "' of type " + type.getName(), e);
            return null;
        }
    }

    /* =========================================================
    Security helpers
    ========================================================= */

    /** Ensure there is an authenticated user, re-using an injected {@link Authentication} if needed. */
    public Authentication authenticate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null && getAuth() != null) {
            authentication =
                    new UsernamePasswordAuthenticationToken(this.auth.getName(), null, this.auth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        return authentication;
    }

    private GeoServerSecurityManager getSecurityManager() {
        return context.getBean(GeoServerSecurityManager.class);
    }

    private void assertAdminOrThrow() {
        final Authentication a = SecurityContextHolder.getContext().getAuthentication();
        final boolean isAdmin = getSecurityManager().checkAuthenticationForAdminRole(a);
        if (!isAdmin) {
            throw new IllegalStateException("Not enough privileges to run a Backup/Restore process!");
        }
    }

    /* =========================================================
    Execution tracking
    ========================================================= */

    /** @return running backup execution IDs (empty if the job is unknown) */
    public Set<Long> getBackupRunningExecutions() {
        try {
            return jobOperator.getRunningExecutions(BACKUP_JOB_NAME);
        } catch (NoSuchJobException e) {
            return Set.of();
        }
    }

    /** @return running restore execution IDs (empty if the job is unknown) */
    public Set<Long> getRestoreRunningExecutions() {
        try {
            return jobOperator.getRunningExecutions(RESTORE_JOB_NAME);
        } catch (NoSuchJobException e) {
            return Set.of();
        }
    }

    /* =========================================================
    Public API - BACKUP
    ========================================================= */

    public BackupExecutionAdapter runBackupAsync(
            final Resource archiveFile,
            final boolean overwrite,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            final Map<String, String> params)
            throws IOException {

        JobParametersBuilder builder = new JobParametersBuilder();
        params.forEach(builder::addString);
        return runBackupAsync(archiveFile, overwrite, wsFilter, siFilter, liFilter, builder);
    }

    public BackupExecutionAdapter runBackupAsync(
            final Resource archiveFile,
            final boolean overwrite,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            final Hints hints)
            throws IOException {

        JobParametersBuilder builder = new JobParametersBuilder();
        parseParams(hints, builder);
        return runBackupAsync(archiveFile, overwrite, wsFilter, siFilter, liFilter, builder);
    }

    private BackupExecutionAdapter runBackupAsync(
            final Resource archiveFile,
            final boolean overwrite,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            final JobParametersBuilder paramsBuilder)
            throws IOException {

        assertAdminOrThrow();
        prepareArchiveTarget(archiveFile, overwrite);

        // Prepare staging directory
        Resource tmpDir = BackupUtils.geoServerTmpDir(getGeoServerDataDirectory());

        if (wsFilter != null) paramsBuilder.addString("wsFilter", ECQL.toCQL(wsFilter));
        if (siFilter != null) paramsBuilder.addString("siFilter", ECQL.toCQL(siFilter));
        if (liFilter != null) paramsBuilder.addString("liFilter", ECQL.toCQL(liFilter));

        paramsBuilder
                .addString(PARAM_JOB_NAME, BACKUP_JOB_NAME)
                .addString(PARAM_OUTPUT_FILE_PATH, BackupUtils.getArchiveURLProtocol(tmpDir) + tmpDir.path())
                .addLong(PARAM_TIME, System.currentTimeMillis());

        final JobParameters jobParameters = paramsBuilder.toJobParameters();
        ensureNoOtherJobsRunning(BACKUP_JOB_NAME);

        try {
            LOGGER.info(() -> "Starting backup job with params: " + jobParameters.getParameters());
            JobExecution jobExecution = jobLauncher.run(backupJob, jobParameters);

            var backupExecution = new BackupExecutionAdapter(jobExecution, totalNumberOfBackupSteps);
            backupExecutions.put(backupExecution.getId(), backupExecution);

            backupExecution.setJobExplorer(jobExplorer);
            backupExecution.setArchiveFile(archiveFile);
            backupExecution.setOverwrite(overwrite);
            backupExecution.setWsFilter(wsFilter);
            backupExecution.setSiFilter(siFilter);
            backupExecution.setLiFilter(liFilter);

            for (Entry<String, ?> jobParam : jobParameters.getParameters().entrySet()) {
                String k = jobParam.getKey();
                if (!PARAM_OUTPUT_FILE_PATH.equals(k) && !PARAM_INPUT_FILE_PATH.equals(k) && !PARAM_TIME.equals(k)) {
                    backupExecution.getOptions().add(k + "=" + jobParam.getValue());
                }
            }

            return backupExecution;
        } catch (JobExecutionAlreadyRunningException
                | JobRestartException
                | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException e) {
            throw new IOException("Could not start a new Backup Job Execution", e);
        }
    }

    /* =========================================================
    Public API - RESTORE
    ========================================================= */

    public RestoreExecutionAdapter runRestoreAsync(
            final Resource archiveFile,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            final Map<String, String> params)
            throws IOException {

        JobParametersBuilder builder = new JobParametersBuilder();
        params.forEach(builder::addString);
        return runRestoreAsync(archiveFile, wsFilter, siFilter, liFilter, builder);
    }

    public RestoreExecutionAdapter runRestoreAsync(
            final Resource archiveFile,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            final Hints params)
            throws IOException {

        JobParametersBuilder builder = new JobParametersBuilder();
        parseParams(params, builder);
        return runRestoreAsync(archiveFile, wsFilter, siFilter, liFilter, builder);
    }

    private RestoreExecutionAdapter runRestoreAsync(
            final Resource archiveFile,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            final JobParametersBuilder paramsBuilder)
            throws IOException {

        assertAdminOrThrow();

        // Extract archive into a temp dir for the job to consume
        Resource tmpDir = BackupUtils.geoServerTmpDir(getGeoServerDataDirectory());
        BackupUtils.extractTo(archiveFile, tmpDir);

        if (wsFilter != null) paramsBuilder.addString("wsFilter", ECQL.toCQL(wsFilter));
        if (siFilter != null) paramsBuilder.addString("siFilter", ECQL.toCQL(siFilter));
        if (liFilter != null) paramsBuilder.addString("liFilter", ECQL.toCQL(liFilter));

        paramsBuilder
                .addString(PARAM_JOB_NAME, RESTORE_JOB_NAME)
                .addString(PARAM_INPUT_FILE_PATH, BackupUtils.getArchiveURLProtocol(tmpDir) + tmpDir.path())
                .addLong(PARAM_TIME, System.currentTimeMillis());

        final JobParameters jobParameters = paramsBuilder.toJobParameters();
        ensureNoOtherJobsRunning(RESTORE_JOB_NAME);

        try {
            LOGGER.info(() -> "Starting restore job with params: " + jobParameters.getParameters());
            JobExecution jobExecution = jobLauncher.run(restoreJob, jobParameters);

            var restoreExecution = new RestoreExecutionAdapter(jobExecution, totalNumberOfRestoreSteps);
            restoreExecutions.put(restoreExecution.getId(), restoreExecution);

            restoreExecution.setJobExplorer(jobExplorer);
            restoreExecution.setArchiveFile(archiveFile);
            restoreExecution.setWsFilter(wsFilter);
            restoreExecution.setSiFilter(siFilter);
            restoreExecution.setLiFilter(liFilter);

            for (Entry<String, ?> jobParam : jobParameters.getParameters().entrySet()) {
                String k = jobParam.getKey();
                if (!PARAM_OUTPUT_FILE_PATH.equals(k) && !PARAM_INPUT_FILE_PATH.equals(k) && !PARAM_TIME.equals(k)) {
                    restoreExecution.getOptions().add(k + "=" + jobParam.getValue());
                }
            }

            return restoreExecution;
        } catch (JobExecutionAlreadyRunningException
                | JobRestartException
                | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException e) {
            throw new IOException("Could not start a new Restore Job Execution", e);
        }
    }

    /* =========================================================
    Job control (stop / restart / abandon)
    ========================================================= */

    /** Stop a running Backup/Restore execution by id. */
    public void stopExecution(Long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
        LOGGER.info(() -> "Stopping execution id=[" + executionId + "]");
        try {
            jobOperator.stop(executionId);
        } finally {
            JobExecution je = jobExplorer.getJobExecution(executionId);
            if (je != null) {
                je.setStatus(BatchStatus.STOPPING);
                je.setEndTime(LocalDateTime.now());
                jobRepository.update(je);
            }
            endRequestCallbacksQuietly();
        }
    }

    /** Restarts a previously executed job instance (if restartable). */
    public Long restartExecution(Long executionId)
            throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException, NoSuchJobException,
                    JobRestartException, JobParametersInvalidException {
        return jobOperator.restart(executionId);
    }

    /** Mark a job execution as abandoned. */
    public void abandonExecution(Long executionId)
            throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException {
        LOGGER.info(() -> "Abandoning execution id=[" + executionId + "]");
        try {
            jobOperator.abandon(executionId);
        } finally {
            JobExecution je = jobExplorer.getJobExecution(executionId);
            if (je != null) {
                je.setStatus(BatchStatus.ABANDONED);
                je.setEndTime(LocalDateTime.now());
                jobRepository.update(je);
            }
            endRequestCallbacksQuietly();
        }
    }

    /* =========================================================
    JobExecutionListener (begin/end request hooks)
    ========================================================= */

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Acquire GeoServer Configuration Lock in READ mode
        List<BackupRestoreCallback> callbacks = GeoServerExtensions.extensions(BackupRestoreCallback.class);
        for (BackupRestoreCallback callback : callbacks) {
            try {
                callback.onBeginRequest(jobExecution.getJobParameters().getString(PARAM_JOB_NAME));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "onBeginRequest callback failed", e);
            }
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        endRequestCallbacksQuietly();
    }

    private void endRequestCallbacksQuietly() {
        try {
            List<BackupRestoreCallback> callbacks = GeoServerExtensions.extensions(BackupRestoreCallback.class);
            for (BackupRestoreCallback callback : callbacks) {
                try {
                    callback.onEndRequest();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "onEndRequest callback failed", e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not unlock GeoServer Catalog Configuration", e);
        }
    }

    /* =========================================================
    Parameters / XStream utils
    ========================================================= */

    /** Translate {@link Hints} to JobParameters. For boolean-like flags we set "true" if present. */
    private void parseParams(final Hints params, JobParametersBuilder b) {
        if (params == null) return;

        for (Entry<Object, Object> param : params.entrySet()) {
            if (param.getKey() instanceof Hints.OptionKey ok) {
                for (String k : ok.getOptions()) {
                    switch (k) {
                        case PARAM_EXCLUDE_FILE_PATH:
                        case PARAM_PASSWORD_TOKENS:
                            b.addString(k, String.valueOf(param.getValue()));
                            break;
                        case PARAM_PARAMETERIZE_PASSWDS:
                        case PARAM_SKIP_SETTINGS:
                        case PARAM_SKIP_SECURITY_SETTINGS:
                        case PARAM_CLEANUP_TEMP:
                        case PARAM_DRY_RUN_MODE:
                        case PARAM_BEST_EFFORT_MODE:
                        case PARAM_SKIP_GWC:
                            if (b.toJobParameters().getString(k) == null) {
                                b.addString(k, "true");
                            }
                            break;
                        default:
                            // ignore unknown keys
                            break;
                    }
                }
            }
        }
    }

    /** Create an XML persister configured for the current catalog. */
    public XStreamPersister createXStreamPersisterXML() {
        return initXStreamPersister(xpf.createXMLPersister());
    }

    /** Create a JSON persister configured for the current catalog. */
    public XStreamPersister createXStreamPersisterJSON() {
        return initXStreamPersister(xpf.createJSONPersister());
    }

    public XStreamPersister initXStreamPersister(XStreamPersister xp) {
        xp.setCatalog(catalog);
        XStream xs = xp.getXStream();

        // Security: allow only required types
        xs.alias("backup", BackupExecutionAdapter.class);
        xs.allowTypes(new Class[] {BackupExecutionAdapter.class});
        xs.allowTypeHierarchy(Resource.class);

        return xp;
    }

    /* =========================================================
    Helpers
    ========================================================= */

    private void prepareArchiveTarget(Resource archiveFile, boolean overwrite) throws IOException {
        Objects.requireNonNull(archiveFile, "archiveFile must not be null");

        if (archiveFile.file().exists()) {
            if (!overwrite && FileUtils.sizeOf(archiveFile.file()) > 0) {
                throw new IOException("The target archive file already exists. "
                        + "Use 'overwrite=TRUE' if you want to overwrite it.");
            }
            FileUtils.forceDelete(archiveFile.file());
        } else {
            if (!archiveFile.file().getParentFile().exists()
                    && !archiveFile.file().getParentFile().mkdirs()) {
                throw new IOException("The path to target archive file is unreachable.");
            }
        }

        // Touch the file to verify the path is writable
        FileUtils.touch(archiveFile.file());
    }

    private void ensureNoOtherJobsRunning(String thisJobName) throws IOException {
        boolean otherBackup = !getBackupRunningExecutions().isEmpty() && !BACKUP_JOB_NAME.equals(thisJobName);
        boolean otherRestore = !getRestoreRunningExecutions().isEmpty() && !RESTORE_JOB_NAME.equals(thisJobName);
        if (otherBackup || otherRestore) {
            throw new IOException(
                    "Could not start a new " + thisJobName + " execution since there are currently running jobs.");
        }
    }

    /** Convenience for tests/tools: get the item name used by XStream for a class. */
    protected String getItemName(XStreamPersister xp, Class<?> clazz) {
        return xp.getClassAliasingMapper().serializedClass(clazz);
    }
}
