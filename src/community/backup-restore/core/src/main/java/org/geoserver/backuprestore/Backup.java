/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
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
 * @author Alessio Fabiani, GeoSolutions
 */
@SuppressWarnings("rawtypes")
public class Backup implements DisposableBean, ApplicationContextAware, ApplicationListener, JobExecutionListener {

    public static final String PARAM_PASSWORD_TOKENS = "BK_PASSWORD_TOKENS";

    public static final String PARAM_SKIP_SETTINGS = "BK_SKIP_SETTINGS";

    public static final String PARAM_SKIP_SECURITY_SETTINGS = "BK_SKIP_SECURITY";

    public static final String PARAM_PURGE_RESOURCES = "BK_PURGE_RESOURCES";

    public static final String PARAM_SKIP_GWC = "BK_SKIP_GWC";

    /**
     * When {@code true}, a restore MERGES the archive's user/group and role data into the target's existing security
     * services (keeping the target's configuration, keystore and master password) instead of replacing the whole
     * security folder. This makes a cross-instance security migration possible even when the target's master password
     * differs from the source's, where a verbatim keystore copy would be unreadable.
     */
    public static final String PARAM_MERGE_SECURITY = "BK_MERGE_SECURITY";

    /**
     * The source instance's master password. When supplied on a security REPLACE restore it lets the archive's
     * keystore be re-encrypted to the target's master password (otherwise the source-encrypted keystore cannot be read
     * on a target with a different master password). Sensitive: handle as a transient parameter.
     */
    public static final String PARAM_SOURCE_MASTER_PASSWORD = "BK_SOURCE_MASTER_PASSWORD";

    static Logger LOGGER = Logging.getLogger(Backup.class);

    /* Job Parameters Keys **/
    public static final String PARAM_TIME = "time";

    public static final String PARAM_JOB_NAME = "job.execution.name";

    public static final String PARAM_OUTPUT_FILE_PATH = "output.file.path";

    public static final String PARAM_INPUT_FILE_PATH = "input.file.path";

    public static final String PARAM_EXCLUDE_FILE_PATH = "exclude.file.path";

    public static final String PARAM_CLEANUP_TEMP = "BK_CLEANUP_TEMP";

    public static final String PARAM_DRY_RUN_MODE = "BK_DRY_RUN";

    public static final String PARAM_BEST_EFFORT_MODE = "BK_BEST_EFFORT";

    /* Jobs Context Keys **/
    public static final String BACKUP_JOB_NAME = "backupJob";

    public static final String RESTORE_JOB_NAME = "restoreJob";

    public static final String PARAM_PARAMETERIZE_PASSWDS = "BK_PARAM_PASSWORDS";

    /**
     * When {@code true}, the backup keeps catalog object ids and writes cross-references by id, instead of stripping
     * ids and referencing by name. The resulting archive can be restored / migrated into another catalog with the
     * original identities preserved, which lets the restore re-link GWC tile layers (they key strictly by id) and skip
     * objects that already exist by id rather than merging by name. The same flag must be passed on the matching
     * restore so the reader expects id references. The default ({@code false}) keeps the legacy portable, name-based
     * archive format and behaviour.
     */
    public static final String PARAM_PRESERVE_IDS = "BK_PRESERVE_IDS";

    public static final String RESTORE_CATALOG_KEY = "restore.catalog";

    private Authentication auth;

    /** catalog */
    Catalog catalog;

    GeoServer geoServer;

    GeoServerResourceLoader resourceLoader;

    GeoServerDataDirectory geoServerDataDirectory;

    XStreamPersisterFactory xpf;

    JobOperator jobOperator;

    JobRepository jobRepository;

    Job backupJob;

    Job restoreJob;

    ConcurrentHashMap<Long, BackupExecutionAdapter> backupExecutions =
            new ConcurrentHashMap<Long, BackupExecutionAdapter>();

    ConcurrentHashMap<Long, RestoreExecutionAdapter> restoreExecutions =
            new ConcurrentHashMap<Long, RestoreExecutionAdapter>();

    Integer totalNumberOfBackupSteps;

    Integer totalNumberOfRestoreSteps;

    /** A static application context */
    private static ApplicationContext context;

    public Backup(Catalog catalog, GeoServerResourceLoader rl) {
        this.catalog = catalog;
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);

        this.resourceLoader = rl;
        this.geoServerDataDirectory = new GeoServerDataDirectory(rl);

        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
    }

    /** @return the context */
    public static ApplicationContext getContext() {
        return context;
    }

    /** @return the jobOperator */
    public JobOperator getJobOperator() {
        return jobOperator;
    }

    /** @return the Backup job */
    public Job getBackupJob() {
        return backupJob;
    }

    /** @return the Restore job */
    public Job getRestoreJob() {
        return restoreJob;
    }

    /** @return the backupExecutions */
    public ConcurrentHashMap<Long, BackupExecutionAdapter> getBackupExecutions() {
        return backupExecutions;
    }

    /** @return the restoreExecutions */
    public ConcurrentHashMap<Long, RestoreExecutionAdapter> getRestoreExecutions() {
        return restoreExecutions;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // load the context store here to avoid circular dependency on creation
        if (event instanceof ContextLoadedEvent) {
            this.jobOperator = (JobOperator) context.getBean("jobOperator");
            this.jobRepository = (JobRepository) context.getBean("jobRepository");
            this.backupJob = (Job) context.getBean(BACKUP_JOB_NAME);
            this.restoreJob = (Job) context.getBean(RESTORE_JOB_NAME);
        }
    }

    /** @return */
    public Set<Long> getBackupRunningExecutions() {
        synchronized (jobOperator) {
            Set<Long> runningExecutions = new HashSet<>();
            for (JobExecution execution : jobRepository.findRunningJobExecutions(BACKUP_JOB_NAME)) {
                runningExecutions.add(execution.getId());
            }
            return runningExecutions;
        }
    }

    /** @return */
    public Set<Long> getRestoreRunningExecutions() {
        synchronized (jobOperator) {
            Set<Long> runningExecutions = new HashSet<>();
            for (JobExecution execution : jobRepository.findRunningJobExecutions(RESTORE_JOB_NAME)) {
                runningExecutions.add(execution.getId());
            }
            return runningExecutions;
        }
    }

    /** @return the auth */
    public Authentication getAuth() {
        return auth;
    }

    /** @param auth the auth to set */
    public void setAuth(Authentication auth) {
        this.auth = auth;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public GeoServer getGeoServer() {
        return geoServer;
    }

    /** @return the resourceLoader */
    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /** @param resourceLoader the resourceLoader to set */
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /** @return the geoServerDataDirectory */
    public GeoServerDataDirectory getGeoServerDataDirectory() {
        return geoServerDataDirectory;
    }

    /** @param geoServerDataDirectory the geoServerDataDirectory to set */
    public void setGeoServerDataDirectory(GeoServerDataDirectory geoServerDataDirectory) {
        this.geoServerDataDirectory = geoServerDataDirectory;
    }

    /** @return the totalNumberOfBackupSteps */
    public Integer getTotalNumberOfBackupSteps() {
        return totalNumberOfBackupSteps;
    }

    /** @param totalNumberOfBackupSteps the totalNumberOfBackupSteps to set */
    public void setTotalNumberOfBackupSteps(Integer totalNumberOfBackupSteps) {
        this.totalNumberOfBackupSteps = totalNumberOfBackupSteps;
    }

    /** @return the totalNumberOfRestoreSteps */
    public Integer getTotalNumberOfRestoreSteps() {
        return totalNumberOfRestoreSteps;
    }

    /** @param totalNumberOfRestoreSteps the totalNumberOfRestoreSteps to set */
    public void setTotalNumberOfRestoreSteps(Integer totalNumberOfRestoreSteps) {
        this.totalNumberOfRestoreSteps = totalNumberOfRestoreSteps;
    }

    @Override
    public void destroy() throws Exception {
        // Nothing to do.
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        Backup.context = context;

        try {
            AbstractJob backupJob = (AbstractJob) context.getBean(BACKUP_JOB_NAME);
            if (backupJob != null) {
                this.setTotalNumberOfBackupSteps(backupJob.getStepNames().size());
            }

            AbstractJob restoreJob = (AbstractJob) context.getBean(BACKUP_JOB_NAME);
            if (restoreJob != null) {
                this.setTotalNumberOfRestoreSteps(restoreJob.getStepNames().size());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not fully configure the Backup Facade!", e);
        }
    }

    /** Authenticate a user */
    public Authentication authenticate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null && getAuth() != null) {
            authentication =
                    new UsernamePasswordAuthenticationToken(this.auth.getName(), null, this.auth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        return authentication;
    }

    protected String getItemName(XStreamPersister xp, Class clazz) {
        return xp.getClassAliasingMapper().serializedClass(clazz);
    }

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

    /** */
    private BackupExecutionAdapter runBackupAsync(
            final Resource archiveFile,
            final boolean overwrite,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            final JobParametersBuilder paramsBuilder)
            throws IOException {

        // Check whether the user is authenticated or not and, in the second case, if it is an
        // Administrator or not
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = getSecurityManager().checkAuthenticationForAdminRole(auth);

        if (!isAdmin) {
            throw new IllegalStateException("Not enough privileges to run a Restore process!");
        }

        // Check if archiveFile exists
        if (archiveFile.file().exists()) {
            if (!overwrite && FileUtils.sizeOf(archiveFile.file()) > 0) {
                // Unless the user explicitly wants to overwrite the archiveFile, throw an exception
                // whenever it already exists
                throw new IOException(
                        "The target archive file already exists. Use 'overwrite=TRUE' if you want to overwrite it.");
            } else {
                FileUtils.forceDelete(archiveFile.file());
            }
        } else {
            // Make sure the parent path exists
            if (!archiveFile.file().getParentFile().exists()) {
                try {
                    archiveFile.file().getParentFile().mkdirs();
                } finally {
                    if (!archiveFile.file().getParentFile().exists()) {
                        throw new IOException("The path to target archive file is unreachable.");
                    }
                }
            }
        }

        // Initialize ZIP
        FileUtils.touch(archiveFile.file());

        // Write flat files into a temporary folder
        Resource tmpDir = BackupUtils.geoServerTmpDir(getGeoServerDataDirectory());

        if (wsFilter != null) {
            paramsBuilder.addString("wsFilter", ECQL.toCQL(wsFilter));
        }
        if (siFilter != null) {
            paramsBuilder.addString("siFilter", ECQL.toCQL(siFilter));
        }
        if (liFilter != null) {
            paramsBuilder.addString("liFilter", ECQL.toCQL(liFilter));
        }

        paramsBuilder
                .addString(PARAM_JOB_NAME, BACKUP_JOB_NAME)
                .addString(PARAM_OUTPUT_FILE_PATH, BackupUtils.getArchiveURLProtocol(tmpDir) + tmpDir.path())
                .addLong(PARAM_TIME, System.currentTimeMillis());

        //        parseParams(params, paramsBuilder);

        JobParameters jobParameters = paramsBuilder.toJobParameters();

        // Send Execution Signal
        BackupExecutionAdapter backupExecution;
        try {
            if (getRestoreRunningExecutions().isEmpty()
                    && getBackupRunningExecutions().isEmpty()) {
                synchronized (jobOperator) {
                    // Start a new Job
                    JobExecution jobExecution = jobOperator.start(backupJob, jobParameters);
                    backupExecution = new BackupExecutionAdapter(jobExecution, totalNumberOfBackupSteps);
                    backupExecutions.put(backupExecution.getId(), backupExecution);

                    backupExecution.setArchiveFile(archiveFile);
                    backupExecution.setOverwrite(overwrite);
                    backupExecution.setWsFilter(wsFilter);
                    backupExecution.setSiFilter(siFilter);
                    backupExecution.setLiFilter(liFilter);

                    backupExecution.getOptions().add("OVERWRITE=" + overwrite);
                    for (JobParameter<?> jobParam : jobParameters) {
                        String key = jobParam.name();
                        if (!PARAM_OUTPUT_FILE_PATH.equals(key)
                                && !PARAM_INPUT_FILE_PATH.equals(key)
                                && !PARAM_TIME.equals(key)) {
                            backupExecution.getOptions().add(key + "=" + jobParam.value());
                        }
                    }

                    return backupExecution;
                }
            } else {
                throw new IOException(
                        "Could not start a new Backup Job Execution since there are currently Running jobs.");
            }
        } catch (JobExecutionAlreadyRunningException
                | JobRestartException
                | JobInstanceAlreadyCompleteException
                | InvalidJobParametersException e) {
            throw new IOException("Could not start a new Backup Job Execution: ", e);
        } finally {
        }
    }

    public RestoreExecutionAdapter runRestoreAsync(
            final Resource archiveFile,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            final Map<String, String> params)
            throws IOException {

        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        params.forEach(paramsBuilder::addString);
        return runRestoreAsync(archiveFile, wsFilter, siFilter, liFilter, paramsBuilder);
    }

    /** */
    public RestoreExecutionAdapter runRestoreAsync(
            final Resource archiveFile,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            final Hints params)
            throws IOException {
        // Fill Job Parameters
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        parseParams(params, paramsBuilder);

        return runRestoreAsync(archiveFile, wsFilter, siFilter, liFilter, paramsBuilder);
    }

    private RestoreExecutionAdapter runRestoreAsync(
            Resource archiveFile,
            final Filter wsFilter,
            final Filter siFilter,
            final Filter liFilter,
            JobParametersBuilder paramsBuilder)
            throws IOException {

        // Check whether the user is authenticated or not and, in the second case, if it is an
        // Administrator or not
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = getSecurityManager().checkAuthenticationForAdminRole(auth);

        if (!isAdmin) {
            throw new IllegalStateException("Not enough privileges to run a Restore process!");
        }

        Resource tmpDir = BackupUtils.geoServerTmpDir(getGeoServerDataDirectory());
        BackupUtils.extractTo(archiveFile, tmpDir);
        RestoreExecutionAdapter restoreExecution;

        if (wsFilter != null) {
            paramsBuilder.addString("wsFilter", ECQL.toCQL(wsFilter));
        }
        if (siFilter != null) {
            paramsBuilder.addString("siFilter", ECQL.toCQL(siFilter));
        }
        if (liFilter != null) {
            paramsBuilder.addString("liFilter", ECQL.toCQL(liFilter));
        }

        paramsBuilder
                .addString(PARAM_JOB_NAME, RESTORE_JOB_NAME)
                .addString(PARAM_INPUT_FILE_PATH, BackupUtils.getArchiveURLProtocol(tmpDir) + tmpDir.path())
                .addLong(PARAM_TIME, System.currentTimeMillis());

        JobParameters jobParameters = paramsBuilder.toJobParameters();

        try {
            if (getRestoreRunningExecutions().isEmpty()
                    && getBackupRunningExecutions().isEmpty()) {
                synchronized (jobOperator) {
                    // Start a new Job
                    JobExecution jobExecution = jobOperator.start(restoreJob, jobParameters);
                    restoreExecution = new RestoreExecutionAdapter(jobExecution, totalNumberOfRestoreSteps);
                    restoreExecutions.put(restoreExecution.getId(), restoreExecution);
                    restoreExecution.setArchiveFile(archiveFile);
                    restoreExecution.setWsFilter(wsFilter);
                    restoreExecution.setSiFilter(siFilter);
                    restoreExecution.setLiFilter(liFilter);

                    for (JobParameter<?> jobParam : jobParameters) {
                        String key = jobParam.name();
                        if (!PARAM_OUTPUT_FILE_PATH.equals(key)
                                && !PARAM_INPUT_FILE_PATH.equals(key)
                                && !PARAM_TIME.equals(key)) {
                            restoreExecution.getOptions().add(key + "=" + jobParam.value());
                        }
                    }

                    return restoreExecution;
                }
            } else {
                throw new IOException(
                        "Could not start a new Restore Job Execution since there are currently Running jobs.");
            }
        } catch (JobExecutionAlreadyRunningException
                | JobRestartException
                | JobInstanceAlreadyCompleteException
                | InvalidJobParametersException e) {
            throw new IOException("Could not start a new Restore Job Execution: ", e);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // Release locks on GeoServer Configuration:
        try {
            List<BackupRestoreCallback> callbacks = GeoServerExtensions.extensions(BackupRestoreCallback.class);
            for (BackupRestoreCallback callback : callbacks) {
                callback.onEndRequest();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not unlock GeoServer Catalog Configuration!", e);
        }
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Acquire GeoServer Configuration Lock in READ mode
        List<BackupRestoreCallback> callbacks = GeoServerExtensions.extensions(BackupRestoreCallback.class);
        for (BackupRestoreCallback callback : callbacks) {
            callback.onBeginRequest(jobExecution.getJobParameters().getString(PARAM_JOB_NAME));
        }
    }

    /** Stop a running Backup/Restore Execution */
    public void stopExecution(Long executionId) throws JobExecutionNotRunningException {
        LOGGER.info("Stopping execution id [" + executionId + "]");

        JobExecution jobExecution = findJobExecution(executionId);
        try {
            if (jobExecution != null) {
                jobOperator.stop(jobExecution);
            } else {
                LOGGER.warning("No job execution found for id " + executionId + "; nothing to stop.");
            }
        } finally {
            if (jobExecution != null) {
                final BatchStatus status = jobExecution.getStatus();

                if (!status.isGreaterThan(BatchStatus.STARTED)) {
                    jobExecution.setStatus(BatchStatus.STOPPING);
                    jobExecution.setEndTime(LocalDateTime.now());
                    jobRepository.update(jobExecution);
                }
            }

            // Release locks on GeoServer Configuration:
            try {
                List<BackupRestoreCallback> callbacks = GeoServerExtensions.extensions(BackupRestoreCallback.class);
                for (BackupRestoreCallback callback : callbacks) {
                    callback.onEndRequest();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Could not unlock GeoServer Catalog Configuration!", e);
            }
        }
    }

    /** Restarts a running Backup/Restore Execution */
    public Long restartExecution(Long executionId) throws JobRestartException {
        JobExecution jobExecution = findJobExecution(executionId);
        if (jobExecution == null) {
            LOGGER.warning("No job execution found for id " + executionId + "; cannot restart.");
            return null;
        }
        return jobOperator.restart(jobExecution).getId();
    }

    /** Abort a running Backup/Restore Execution */
    public void abandonExecution(Long executionId) throws JobExecutionAlreadyRunningException {
        LOGGER.info("Aborting execution id [" + executionId + "]");

        JobExecution jobExecution = findJobExecution(executionId);
        try {
            if (jobExecution != null) {
                jobOperator.abandon(jobExecution);
            } else {
                LOGGER.warning("No job execution found for id " + executionId + "; nothing to abandon.");
            }
        } finally {
            if (jobExecution != null) {
                jobExecution.setStatus(BatchStatus.ABANDONED);
                jobExecution.setEndTime(LocalDateTime.now());
                jobRepository.update(jobExecution);
            }

            // Release locks on GeoServer Configuration:
            try {
                List<BackupRestoreCallback> callbacks = GeoServerExtensions.extensions(BackupRestoreCallback.class);
                for (BackupRestoreCallback callback : callbacks) {
                    callback.onEndRequest();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Could not unlock GeoServer Catalog Configuration!", e);
            }
        }
    }

    /**
     * Resolves a {@link JobExecution} by id from the in-flight backup/restore maps, falling back to the job repository.
     * Returns {@code null} when no execution exists for the id: the JDBC job repository throws rather than returning
     * {@code null} for an unknown id, so that case is normalized to {@code null} here.
     */
    private JobExecution findJobExecution(Long executionId) {
        if (this.backupExecutions.get(executionId) != null) {
            return this.backupExecutions.get(executionId).getDelegate();
        }
        if (this.restoreExecutions.get(executionId) != null) {
            return this.restoreExecutions.get(executionId).getDelegate();
        }
        try {
            return jobRepository.getJobExecution(executionId);
        } catch (org.springframework.dao.DataAccessException e) {
            return null;
        }
    }

    /** */
    private void parseParams(final Hints params, JobParametersBuilder paramsBuilder) {
        if (params != null) {
            for (Entry<Object, Object> param : params.entrySet()) {
                if (param.getKey() instanceof Hints.OptionKey) {
                    final Set<String> key = ((Hints.OptionKey) param.getKey()).getOptions();
                    for (String k : key) {
                        switch (k) {
                            case PARAM_EXCLUDE_FILE_PATH:
                            case PARAM_PASSWORD_TOKENS:
                            case PARAM_SOURCE_MASTER_PASSWORD:
                                paramsBuilder.addString(k, (String) param.getValue());
                                break;
                            case PARAM_PARAMETERIZE_PASSWDS:
                            case PARAM_SKIP_SETTINGS:
                            case PARAM_SKIP_SECURITY_SETTINGS:
                            case PARAM_PURGE_RESOURCES:
                            case PARAM_CLEANUP_TEMP:
                            case PARAM_DRY_RUN_MODE:
                            case PARAM_BEST_EFFORT_MODE:
                            case PARAM_SKIP_GWC:
                            case PARAM_PRESERVE_IDS:
                            case PARAM_MERGE_SECURITY:
                                if (paramsBuilder.toJobParameters().getString(k) == null) {
                                    paramsBuilder.addString(k, booleanOptionValue(param.getValue()));
                                }
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolves the value of a boolean job option supplied through the {@link Hints} API.
     *
     * <p>Historically the only way to pass a boolean option through {@code Hints} was to add the option key with the
     * option <em>name</em> as its value (e.g. {@code OptionKey(BK_SKIP_GWC) -> "BK_SKIP_GWC"}); presence alone meant
     * {@code true} and there was no way to express {@code false}. That made the default-{@code true} options (e.g.
     * {@code BK_SKIP_SECURITY}) impossible to switch off from the UI. This helper keeps that legacy contract (any
     * non-boolean value, including the option name, yields {@code "true"}) while additionally honoring an explicit
     * {@code "true"}/{@code "false"} string, so callers that know the desired state can pass it directly.
     */
    private static String booleanOptionValue(Object rawValue) {
        String value = String.valueOf(rawValue);
        return "false".equalsIgnoreCase(value) ? "false" : "true";
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Single source of truth for the boolean job options whose default is "true".
    //
    // These options used to be read ad-hoc in several steps (CatalogBackupRestoreTasklet,
    // CatalogSecurityManagerTasklet, RestoreJobExecutionListener) with inconsistent defaults — the same option could be
    // treated as "true" in one step and "false" in another within a single run. The helpers below make every step read
    // the option identically, with the documented default of "true": security and global settings are excluded, and a
    // restore purges pre-existing resources, unless the caller explicitly passes "false".
    // -----------------------------------------------------------------------------------------------------------------

    /** Whether security settings are excluded from the backup/restore. Default {@code true} (excluded). */
    public static boolean isSkipSecuritySettings(JobParameters params) {
        return Boolean.parseBoolean(params.getString(PARAM_SKIP_SECURITY_SETTINGS, "true"));
    }

    /** Whether global settings are excluded from the backup/restore. Default {@code true} (excluded). */
    public static boolean isSkipSettings(JobParameters params) {
        return Boolean.parseBoolean(params.getString(PARAM_SKIP_SETTINGS, "true"));
    }

    /**
     * Whether a restore purges pre-existing resources (e.g. drops existing workspaces) before restoring. Default
     * {@code true} (purge) — a restore is destructive by design; pass {@code BK_PURGE_RESOURCES=false} to merge into
     * the existing catalog instead.
     */
    public static boolean isPurgeResources(JobParameters params) {
        return Boolean.parseBoolean(params.getString(PARAM_PURGE_RESOURCES, "true"));
    }

    /**
     * Whether a restore merges the archive's user/group and role data into the target's existing security services
     * (keeping the target's configuration, keystore and master password) instead of replacing the whole security
     * folder. Default {@code false} (replace). See {@link #PARAM_MERGE_SECURITY}.
     */
    public static boolean isMergeSecurity(JobParameters params) {
        return Boolean.parseBoolean(params.getString(PARAM_MERGE_SECURITY, "false"));
    }

    /**
     * The source instance's master password supplied for a security REPLACE restore, or {@code null} when not
     * provided (the keystore is then copied verbatim). See {@link #PARAM_SOURCE_MASTER_PASSWORD}.
     */
    public static String getSourceMasterPassword(JobParameters params) {
        return params.getString(PARAM_SOURCE_MASTER_PASSWORD, null);
    }

    public XStreamPersister createXStreamPersisterXML() {
        return initXStreamPersister(new XStreamPersisterFactory().createXMLPersister());
    }

    public XStreamPersister createXStreamPersisterJSON() {
        return initXStreamPersister(new XStreamPersisterFactory().createJSONPersister());
    }

    public XStreamPersister initXStreamPersister(XStreamPersister xp) {
        xp.setCatalog(catalog);

        XStream xs = xp.getXStream();

        // ImportContext
        xs.alias("backup", BackupExecutionAdapter.class);

        // security
        xs.allowTypes(new Class[] {BackupExecutionAdapter.class});
        xs.allowTypeHierarchy(Resource.class);

        return xp;
    }

    /** @return */
    private GeoServerSecurityManager getSecurityManager() {
        return context.getBean(GeoServerSecurityManager.class);
    }
}
