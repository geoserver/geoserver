/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.backuprestore.executor.BackupAsyncExecutor;
import org.geoserver.backuprestore.executor.RestoreAsyncExecutor;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.*;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
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
 * @author Alessio Fabiani, GeoSolutions
 */
@SuppressWarnings("rawtypes")
public class Backup extends JobExecutionListenerSupport
        implements DisposableBean, ApplicationContextAware, ApplicationListener {

    public static final String PARAM_PASSWORD_TOKENS = "BK_PASSWORD_TOKENS";

    public static final String PARAM_SKIP_SETTINGS = "BK_SKIP_SETTINGS";

    public static final String PARAM_SKIP_SECURITY_SETTINGS = "BK_SKIP_SECURITY";

    public static final String PARAM_PURGE_RESOURCES = "BK_PURGE_RESOURCES";

    public static final String PARAM_SKIP_GWC = "BK_SKIP_GWC";
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
    public static final String RESTORE_CATALOG_KEY = "restore.catalog";
    static Logger LOGGER = Logging.getLogger(Backup.class);
    /** A static application context */
    private static ApplicationContext context;

    private final BackupAsyncExecutor backupAsyncExecutor;
    private final RestoreAsyncExecutor restoreAsyncExecutor;
    /** catalog */
    private final Catalog catalog;

    private final TileLayerCatalog tileLayerCatalog;

    GeoServer geoServer;

    GeoServerResourceLoader resourceLoader;

    GeoServerDataDirectory geoServerDataDirectory;

    XStreamPersisterFactory xpf;

    JobOperator jobOperator;

    JobLauncher jobLauncher;

    JobRepository jobRepository;

    Job backupJob;

    Job restoreJob;

    ConcurrentHashMap<Long, BackupExecutionAdapter> backupExecutions =
            new ConcurrentHashMap<Long, BackupExecutionAdapter>();

    ConcurrentHashMap<Long, RestoreExecutionAdapter> restoreExecutions =
            new ConcurrentHashMap<Long, RestoreExecutionAdapter>();

    Integer totalNumberOfBackupSteps;

    Integer totalNumberOfRestoreSteps;
    private Authentication auth;

    public Backup(Catalog catalog, GeoServerResourceLoader rl) {
        this.catalog = catalog;
        this.resourceLoader = rl;
        this.geoServerDataDirectory = new GeoServerDataDirectory(rl);
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
        this.tileLayerCatalog =
                (TileLayerCatalog) GeoServerExtensions.bean("GeoSeverTileLayerCatalog");
        this.backupAsyncExecutor = new BackupAsyncExecutor(this);
        this.restoreAsyncExecutor = new RestoreAsyncExecutor(this);
    }

    /** @return the context */
    public static ApplicationContext getContext() {
        return context;
    }

    /** @return the jobOperator */
    public JobOperator getJobOperator() {
        return jobOperator;
    }

    /** @return the jobLauncher */
    public JobLauncher getJobLauncher() {
        return jobLauncher;
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
            this.jobLauncher = (JobLauncher) context.getBean("jobLauncherAsync");
            this.jobRepository = (JobRepository) context.getBean("jobRepository");
            this.backupJob = (Job) context.getBean(BACKUP_JOB_NAME);
            this.restoreJob = (Job) context.getBean(RESTORE_JOB_NAME);
        }
    }

    /** @return */
    public Set<Long> getBackupRunningExecutions() {
        synchronized (jobOperator) {
            Set<Long> runningExecutions;
            try {
                runningExecutions = jobOperator.getRunningExecutions(BACKUP_JOB_NAME);
            } catch (NoSuchJobException e) {
                runningExecutions = new HashSet<>();
            }
            return runningExecutions;
        }
    }

    /** @return */
    public Set<Long> getRestoreRunningExecutions() {
        synchronized (jobOperator) {
            Set<Long> runningExecutions;
            try {
                runningExecutions = jobOperator.getRunningExecutions(RESTORE_JOB_NAME);
            } catch (NoSuchJobException e) {
                runningExecutions = new HashSet<>();
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

    public TileLayerCatalog getTileLayerCatalog() {
        return tileLayerCatalog;
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
                    new UsernamePasswordAuthenticationToken(
                            this.auth.getName(), null, this.auth.getAuthorities());
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

        return backupAsyncExecutor.executeAsyncBackup(
                archiveFile, overwrite, wsFilter, siFilter, liFilter, builder);
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
        return backupAsyncExecutor.executeAsyncBackup(
                archiveFile, overwrite, wsFilter, siFilter, liFilter, builder);
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
        return restoreAsyncExecutor.executeAsyncRestore(
                archiveFile, wsFilter, siFilter, liFilter, paramsBuilder);
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

        return restoreAsyncExecutor.executeAsyncRestore(
                archiveFile, wsFilter, siFilter, liFilter, paramsBuilder);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // Release locks on GeoServer Configuration:
        try {
            List<BackupRestoreCallback> callbacks =
                    GeoServerExtensions.extensions(BackupRestoreCallback.class);
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
        List<BackupRestoreCallback> callbacks =
                GeoServerExtensions.extensions(BackupRestoreCallback.class);
        for (BackupRestoreCallback callback : callbacks) {
            callback.onBeginRequest(jobExecution.getJobParameters().getString(PARAM_JOB_NAME));
        }
    }

    /** Stop a running Backup/Restore Execution */
    public void stopExecution(Long executionId)
            throws NoSuchJobExecutionException, JobExecutionNotRunningException {
        LOGGER.info("Stopping execution id [" + executionId + "]");

        JobExecution jobExecution = null;
        try {
            if (this.backupExecutions.get(executionId) != null) {
                jobExecution = this.backupExecutions.get(executionId).getDelegate();
            } else if (this.restoreExecutions.get(executionId) != null) {
                jobExecution = this.restoreExecutions.get(executionId).getDelegate();
            }

            jobOperator.stop(executionId);
        } finally {
            if (jobExecution != null) {
                final BatchStatus status = jobExecution.getStatus();

                if (!status.isGreaterThan(BatchStatus.STARTED)) {
                    jobExecution.setStatus(BatchStatus.STOPPING);
                    jobExecution.setEndTime(new Date());
                    jobRepository.update(jobExecution);
                }
            }

            // Release locks on GeoServer Configuration:
            try {
                List<BackupRestoreCallback> callbacks =
                        GeoServerExtensions.extensions(BackupRestoreCallback.class);
                for (BackupRestoreCallback callback : callbacks) {
                    callback.onEndRequest();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Could not unlock GeoServer Catalog Configuration!", e);
            }
        }
    }

    /** Restarts a running Backup/Restore Execution */
    public Long restartExecution(Long executionId)
            throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException,
                    NoSuchJobException, JobRestartException, JobParametersInvalidException {
        return jobOperator.restart(executionId);
    }

    /** Abort a running Backup/Restore Execution */
    public void abandonExecution(Long executionId)
            throws NoSuchJobExecutionException, JobExecutionAlreadyRunningException {
        LOGGER.info("Aborting execution id [" + executionId + "]");

        JobExecution jobExecution = null;
        try {
            if (this.backupExecutions.get(executionId) != null) {
                jobExecution = this.backupExecutions.get(executionId).getDelegate();
            } else if (this.restoreExecutions.get(executionId) != null) {
                jobExecution = this.restoreExecutions.get(executionId).getDelegate();
            }

            jobOperator.abandon(executionId);
        } finally {
            if (jobExecution != null) {
                jobExecution.setStatus(BatchStatus.ABANDONED);
                jobExecution.setEndTime(new Date());
                jobRepository.update(jobExecution);
            }

            // Release locks on GeoServer Configuration:
            try {
                List<BackupRestoreCallback> callbacks =
                        GeoServerExtensions.extensions(BackupRestoreCallback.class);
                for (BackupRestoreCallback callback : callbacks) {
                    callback.onEndRequest();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Could not unlock GeoServer Catalog Configuration!", e);
            }
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
                                paramsBuilder.addString(k, (String) param.getValue());
                                break;
                            case PARAM_PARAMETERIZE_PASSWDS:
                            case PARAM_SKIP_SETTINGS:
                            case PARAM_SKIP_SECURITY_SETTINGS:
                            case PARAM_CLEANUP_TEMP:
                            case PARAM_DRY_RUN_MODE:
                            case PARAM_BEST_EFFORT_MODE:
                            case PARAM_SKIP_GWC:
                                if (paramsBuilder.toJobParameters().getString(k) == null) {
                                    paramsBuilder.addString(k, "true");
                                }
                        }
                    }
                }
            }
        }
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
    public GeoServerSecurityManager getSecurityManager() {
        return context.getBean(GeoServerSecurityManager.class);
    }
}
