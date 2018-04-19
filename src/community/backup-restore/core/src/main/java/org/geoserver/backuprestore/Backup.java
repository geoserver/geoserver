/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.io.IOException;
import java.util.Date;
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
import org.geotools.factory.Hints;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
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

import com.thoughtworks.xstream.XStream;

/**
 * Primary controller/facade of the backup and restore subsystem.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
@SuppressWarnings("rawtypes")
public class Backup extends JobExecutionListenerSupport
        implements DisposableBean, ApplicationContextAware, ApplicationListener {

    public static final String PARAM_PASSWORD_TOKENS = "BK_PASSWORD_TOKENS";

    public static final String PARAM_SKIP_SETTINGS = "BK_SKIP_SETTINGS";

    public static final String PARAM_SKIP_SECURITY_SETTINGS = "BK_SKIP_SECURITY";

    public static final String PARAM_PURGE_RESOURCES = "BK_PURGE_RESOURCES";

    public static final String PARAM_SKIP_GWC = "BK_SKIP_GWC";

    static Logger LOGGER = Logging.getLogger(Backup.class);

    /* Job Parameters Keys **/
    public static final String PARAM_TIME = "time";
    
    public static final String PARAM_JOB_NAME = "job.execution.name";

    public static final String PARAM_OUTPUT_FILE_PATH = "output.file.path";

    public static final String PARAM_INPUT_FILE_PATH = "input.file.path";

    public static final String PARAM_CLEANUP_TEMP = "BK_CLEANUP_TEMP";
    
    public static final String PARAM_DRY_RUN_MODE = "BK_DRY_RUN";

    public static final String PARAM_BEST_EFFORT_MODE = "BK_BEST_EFFORT";

    /* Jobs Context Keys **/
    public static final String BACKUP_JOB_NAME = "backupJob";

    public static final String RESTORE_JOB_NAME = "restoreJob";

    public static final String PARAM_PARAMETERIZE_PASSWDS = "BK_PARAM_PASSWORDS";

    public static final String RESTORE_CATALOG_KEY = "restore.catalog";

    /** catalog */
    Catalog catalog;

    GeoServer geoServer;

    GeoServerResourceLoader resourceLoader;

    GeoServerDataDirectory geoServerDataDirectory;

    XStreamPersisterFactory xpf;

    JobOperator jobOperator;

    JobLauncher jobLauncher;

    JobRepository jobRepository;

    Job backupJob;

    Job restoreJob;

    ConcurrentHashMap<Long, BackupExecutionAdapter> backupExecutions = new ConcurrentHashMap<Long, BackupExecutionAdapter>();

    ConcurrentHashMap<Long, RestoreExecutionAdapter> restoreExecutions = new ConcurrentHashMap<Long, RestoreExecutionAdapter>();

    Integer totalNumberOfBackupSteps;

    Integer totalNumberOfRestoreSteps;

    /**
     * A static application context
     */
    private static ApplicationContext context;

    public Backup(Catalog catalog, GeoServerResourceLoader rl) {
        this.catalog = catalog;
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);

        this.resourceLoader = rl;
        this.geoServerDataDirectory = new GeoServerDataDirectory(rl);

        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
    }

    /**
     * @return the context
     */
    public static ApplicationContext getContext() {
        return context;
    }

    /**
     * @return the jobOperator
     */
    public JobOperator getJobOperator() {
        return jobOperator;
    }

    /**
     * @return the jobLauncher
     */
    public JobLauncher getJobLauncher() {
        return jobLauncher;
    }

    /**
     * @return the Backup job
     */
    public Job getBackupJob() {
        return backupJob;
    }

    /**
     * @return the Restore job
     */
    public Job getRestoreJob() {
        return restoreJob;
    }

    /**
     * @return the backupExecutions
     */
    public ConcurrentHashMap<Long, BackupExecutionAdapter> getBackupExecutions() {
        return backupExecutions;
    }

    /**
     * @return the restoreExecutions
     */
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

    /**
     * @return
     */
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

    /**
     * @return
     */
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

    public Catalog getCatalog() {
        return catalog;
    }

    public GeoServer getGeoServer() {
        return geoServer;
    }

    /**
     * @return the resourceLoader
     */
    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /**
     * @param resourceLoader the resourceLoader to set
     */
    public void setResourceLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * @return the geoServerDataDirectory
     */
    public GeoServerDataDirectory getGeoServerDataDirectory() {
        return geoServerDataDirectory;
    }

    /**
     * @param geoServerDataDirectory the geoServerDataDirectory to set
     */
    public void setGeoServerDataDirectory(GeoServerDataDirectory geoServerDataDirectory) {
        this.geoServerDataDirectory = geoServerDataDirectory;
    }

    /**
     * @return the totalNumberOfBackupSteps
     */
    public Integer getTotalNumberOfBackupSteps() {
        return totalNumberOfBackupSteps;
    }

    /**
     * @param totalNumberOfBackupSteps the totalNumberOfBackupSteps to set
     */
    public void setTotalNumberOfBackupSteps(Integer totalNumberOfBackupSteps) {
        this.totalNumberOfBackupSteps = totalNumberOfBackupSteps;
    }

    /**
     * @return the totalNumberOfRestoreSteps
     */
    public Integer getTotalNumberOfRestoreSteps() {
        return totalNumberOfRestoreSteps;
    }

    /**
     * @param totalNumberOfRestoreSteps the totalNumberOfRestoreSteps to set
     */
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

    protected String getItemName(XStreamPersister xp, Class clazz) {
        return xp.getClassAliasingMapper().serializedClass(clazz);
    }

    public BackupExecutionAdapter runBackupAsync(final Resource archiveFile,
        final boolean overwrite, final Filter filter, final Map<String,String> params) throws IOException {

        JobParametersBuilder builder = new JobParametersBuilder();
        params.forEach(builder::addString);

        return runBackupAsync(archiveFile, overwrite, filter, builder);
    }


    public BackupExecutionAdapter runBackupAsync(final Resource archiveFile,
        final boolean overwrite, final Filter filter, final Hints hints) throws IOException {

        JobParametersBuilder builder = new JobParametersBuilder();
        parseParams(hints, builder);
        return runBackupAsync(archiveFile, overwrite, filter, builder);
    }

    /**
     * @return
     * @throws IOException
     * 
     */
    public BackupExecutionAdapter runBackupAsync(final Resource archiveFile,
            final boolean overwrite, final Filter filter, final JobParametersBuilder paramsBuilder) throws IOException {
        // Check if archiveFile exists
        if (archiveFile.file().exists()) {
            if (!overwrite && FileUtils.sizeOf(archiveFile.file()) > 0) {
                // Unless the user explicitly wants to overwrite the archiveFile, throw an exception whenever it already exists
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

        if (filter != null) {
            paramsBuilder.addString("filter", ECQL.toCQL(filter));
        }

        paramsBuilder
                .addString(PARAM_JOB_NAME, BACKUP_JOB_NAME)
                .addString(PARAM_OUTPUT_FILE_PATH,
                        BackupUtils.getArchiveURLProtocol(tmpDir) + tmpDir.path())
                .addLong(PARAM_TIME, System.currentTimeMillis());

//        parseParams(params, paramsBuilder);

        JobParameters jobParameters = paramsBuilder.toJobParameters();

        // Send Execution Signal
        BackupExecutionAdapter backupExecution;
        try {
            if (getRestoreRunningExecutions().isEmpty() && getBackupRunningExecutions().isEmpty()) {
                synchronized (jobOperator) {
                    // Start a new Job
                    JobExecution jobExecution = jobLauncher.run(backupJob, jobParameters);
                    backupExecution = new BackupExecutionAdapter(jobExecution,
                            totalNumberOfBackupSteps);
                    backupExecutions.put(backupExecution.getId(), backupExecution);

                    backupExecution.setArchiveFile(archiveFile);
                    backupExecution.setOverwrite(overwrite);
                    backupExecution.setFilter(filter);

                    backupExecution.getOptions().add("OVERWRITE=" + overwrite);
                    for (Entry jobParam : jobParameters.toProperties().entrySet()) {
                        if (!PARAM_OUTPUT_FILE_PATH.equals(jobParam.getKey())
                                && !PARAM_INPUT_FILE_PATH.equals(jobParam.getKey())
                                && !PARAM_TIME.equals(jobParam.getKey())) {
                            backupExecution.getOptions()
                                    .add(jobParam.getKey() + "=" + jobParam.getValue());
                        }
                    }

                    return backupExecution;
                }
            } else {
                throw new IOException(
                        "Could not start a new Backup Job Execution since there are currently Running jobs.");
            }
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            throw new IOException("Could not start a new Backup Job Execution: ", e);
        } finally {
        }
    }

    public RestoreExecutionAdapter runRestoreAsync(final Resource archiveFile, final Filter filter,
        final Map<String, String> params) throws IOException {

        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        params.forEach(paramsBuilder::addString);
        return runRestoreAsync(archiveFile, filter, paramsBuilder);
    }

    /**
     * @return
     * @return
     * @throws IOException
     * 
     */
    public RestoreExecutionAdapter runRestoreAsync(final Resource archiveFile, final Filter filter,
            final Hints params) throws IOException {
        // Fill Job Parameters
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        parseParams(params, paramsBuilder);

        return runRestoreAsync(archiveFile, filter, paramsBuilder);
    }

    private RestoreExecutionAdapter runRestoreAsync(Resource archiveFile, Filter filter,
        JobParametersBuilder paramsBuilder) throws IOException {

        Resource tmpDir = BackupUtils.geoServerTmpDir(getGeoServerDataDirectory());
        BackupUtils.extractTo(archiveFile, tmpDir);
        RestoreExecutionAdapter restoreExecution;

        if (filter != null) {
            paramsBuilder.addString("filter", ECQL.toCQL(filter));
        }

        paramsBuilder
            .addString(PARAM_JOB_NAME, RESTORE_JOB_NAME)
            .addString(PARAM_INPUT_FILE_PATH,
                BackupUtils.getArchiveURLProtocol(tmpDir) + tmpDir.path())
            .addLong(PARAM_TIME, System.currentTimeMillis());

        JobParameters jobParameters = paramsBuilder.toJobParameters();

        try {
            if (getRestoreRunningExecutions().isEmpty() && getBackupRunningExecutions().isEmpty()) {
                synchronized (jobOperator) {
                    // Start a new Job
                    JobExecution jobExecution = jobLauncher.run(restoreJob, jobParameters);
                    restoreExecution = new RestoreExecutionAdapter(jobExecution,
                            totalNumberOfRestoreSteps);
                    restoreExecutions.put(restoreExecution.getId(), restoreExecution);
                    restoreExecution.setArchiveFile(archiveFile);
                    restoreExecution.setFilter(filter);

                    for (Entry jobParam : jobParameters.toProperties().entrySet()) {
                        if (!PARAM_OUTPUT_FILE_PATH.equals(jobParam.getKey())
                                && !PARAM_INPUT_FILE_PATH.equals(jobParam.getKey())
                                && !PARAM_TIME.equals(jobParam.getKey())) {
                            restoreExecution.getOptions()
                                    .add(jobParam.getKey() + "=" + jobParam.getValue());
                        }
                    }

                    return restoreExecution;
                }
            } else {
                throw new IOException(
                        "Could not start a new Restore Job Execution since there are currently Running jobs.");
            }
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
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

    /**
     * Stop a running Backup/Restore Execution
     * 
     * @param executionId
     * @return
     * @throws NoSuchJobExecutionException
     * @throws JobExecutionNotRunningException
     */
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
     * Restarts a running Backup/Restore Execution
     * 
     * @param executionId
     * @return
     * @throws JobInstanceAlreadyCompleteException
     * @throws NoSuchJobExecutionException
     * @throws NoSuchJobException
     * @throws JobRestartException
     * @throws JobParametersInvalidException
     */
    public Long restartExecution(Long executionId)
            throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException,
            NoSuchJobException, JobRestartException, JobParametersInvalidException {
        return jobOperator.restart(executionId);
    }

    /**
     * Abort a running Backup/Restore Execution
     * 
     * @param executionId
     * @throws NoSuchJobExecutionException
     * @throws JobExecutionAlreadyRunningException
     */
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
     * @param params
     * @param paramsBuilder
     */
    private void parseParams(final Hints params, JobParametersBuilder paramsBuilder) {
        if (params != null) {
            for (Entry<Object, Object> param : params.entrySet()) {
                if (param.getKey() instanceof Hints.OptionKey) {
                    final Set<String> key = ((Hints.OptionKey) param.getKey()).getOptions();
                    for (String k : key) {
                        switch (k) {
                        case PARAM_PASSWORD_TOKENS:
                            paramsBuilder.addString(k, (String)param.getValue());
                            break;
                        case PARAM_PARAMETERIZE_PASSWDS:
                        case PARAM_SKIP_SETTINGS:
                        case PARAM_CLEANUP_TEMP:                            
                        case PARAM_DRY_RUN_MODE:
                        case PARAM_BEST_EFFORT_MODE:
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
        // xp.setReferenceByName(true);

        XStream xs = xp.getXStream();

        // ImportContext
        xs.alias("backup", BackupExecutionAdapter.class);

        // security
        xs.allowTypes(new Class[] { BackupExecutionAdapter.class });
        xs.allowTypeHierarchy(Resource.class);

        return xp;
    }
}
