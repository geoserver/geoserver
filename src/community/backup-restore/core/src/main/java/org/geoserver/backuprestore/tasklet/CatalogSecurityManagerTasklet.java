/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import java.io.IOException;
import java.util.logging.Level;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecurityManagerListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.Assert;

/**
 * Concrete implementation of the {@link AbstractCatalogBackupRestoreTasklet}.
 * <br>
 * Actually takes care of dumping/restoring GeoServer Security subsystem.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class CatalogSecurityManagerTasklet extends AbstractCatalogBackupRestoreTasklet {

    public static final String SECURITY_RESOURCE_NAME = "security";
    private boolean skipSecuritySettings = false;

    public CatalogSecurityManagerTasklet(Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        super(backupFacade, xStreamPersisterFactory);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        boolean skipSettings = Boolean.parseBoolean(stepExecution.getJobParameters().getString(
            Backup.PARAM_SKIP_SETTINGS));
        boolean skipSecurity = Boolean.parseBoolean(stepExecution.getJobParameters().getString(
            Backup.PARAM_SKIP_SECURITY_SETTINGS));

        this.skipSecuritySettings = skipSettings || skipSecurity;
    }

    @Override
    RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext,
            JobExecution jobExecution) throws Exception {
        final GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();

        // GeoServer Security Folder
        // dd.getResourceStore().get(SECURITY_RESOURCE_NAME);
        final Resource security = dd.getSecurity(Paths.BASE);

        if (!isNew() && !skipSecuritySettings) {
            /*
             * BACKUP Security Resources
             */
            final String outputFolderURL = jobExecution.getJobParameters()
                    .getString(Backup.PARAM_OUTPUT_FILE_PATH);
            final Resource targetBackupFolder = Resources.fromURL(outputFolderURL);
            final Resource securityTargetResource = BackupUtils.dir(targetBackupFolder, SECURITY_RESOURCE_NAME);

            // Copy the Security files into the destination resource
            try {
                Resources.copy(security, securityTargetResource);
            } catch (IOException e) {
                logValidationExceptions((ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!",
                                e));
            }

            // Test that the security folder has been correctly saved
            GeoServerSecurityManager testGssm = null;
            try {
                testGssm = new GeoServerSecurityManager(
                        new GeoServerDataDirectory(targetBackupFolder.dir()));
                testGssm.setApplicationContext(Backup.getContext());
                testGssm.reload();

                // TODO: Perform validation tests here using "testGssm"

                // TODO: Save warnings and validation issues on the JobContext
            } catch (Exception e) {
                logValidationExceptions((ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!",
                                e));
            } finally {
                if (testGssm != null) {
                    try {
                        testGssm.destroy();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Test GeoServerSecurityManager Destry Error!", e);
                    }
                }
            }
        } else if (!skipSecuritySettings) {
            /*
             * RESTORE Security Resources
             */
            
            /**
             * Create a new GeoServerSecurityManager instance using the INPUT DATA DIR.
             * 
             * Try to load the configuration from there and if everything is ok: 
             * 1. Replace the security folders 
             * 2. Destroy and reload the appContext GeoServerSecurityManager 
             * 3. Issue SecurityManagerListener extensions handlePostChanged(...)
             */
            final String inputFolderURL = jobExecution.getJobParameters()
                    .getString(Backup.PARAM_INPUT_FILE_PATH);
            final Resource sourceRestoreFolder = Resources.fromURL(inputFolderURL);
            final Resource sourceSecurityResource = BackupUtils.dir(sourceRestoreFolder, SECURITY_RESOURCE_NAME);

            // Test that the security folder has been correctly saved
            GeoServerSecurityManager testGssm = null;
            try {
                testGssm = new GeoServerSecurityManager(
                        new GeoServerDataDirectory(
                                new GeoServerResourceLoader(sourceRestoreFolder.dir())));
                testGssm.setApplicationContext(Backup.getContext());
                testGssm.reload();

                // TODO: Perform more validation tests here using "testGssm"

                // TODO: Save detailed warnings and validation issues on the JobContext
            } catch (Exception e) {
                logValidationExceptions((ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!",
                                e));
            } finally {
                if (testGssm != null) {
                    try {
                        testGssm.destroy();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Test GeoServerSecurityManager Destry Error!", e);
                    }
                }
            }

            // Copy the Security files into the destination folder
            
            // First of all do a backup of the original security folder
            Resource tmpDir = BackupUtils.tmpDir();
            try {
                Resources.copy(security, tmpDir);
            } catch (IOException e) {
                logValidationExceptions((ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!",
                                new IOException("It was not possible to backup the original Security folder!", e)));
            }
            
            if (Resources.exists(security) && !security.delete()) {
                // Try to restore the original one
                try {
                    Resources.copy(tmpDir, security);
                } catch (IOException e) {
                    logValidationExceptions((ValidationResult) null,
                            new UnexpectedJobExecutionException(
                                    "Exception occurred while storing GeoServer security and services settings!",
                                    new IOException("It was not possible to fully restore the original Security folder!", e)));
                }
                
                logValidationExceptions((ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!",
                                new IOException("It was not possible to cleanup the target security folder!")));
            }
            
            
            // Do this *ONLY* when DRY-RUN-MODE == OFF
            if (!isDryRun()) {
                try {
                    Resources.copy(sourceSecurityResource, security);
                } catch (IOException e) {
                    // Try to restore the original one
                    try {
                        Resources.copy(tmpDir, security);
                    } catch (IOException e1) {
                        logValidationExceptions((ValidationResult) null,
                                new UnexpectedJobExecutionException(
                                        "Exception occurred while storing GeoServer security and services settings!",
                                        new IOException("It was not possible to fully restore the original Security folder!", e1)));
                    }
                    
                    logValidationExceptions((ValidationResult) null,
                            new UnexpectedJobExecutionException(
                                    "Exception occurred while storing GeoServer security and services settings!",
                                    e));
                }
    
                // Reload Security Context
                GeoServerSecurityManager securityContext = GeoServerExtensions
                        .bean(GeoServerSecurityManager.class);
                securityContext.reload();
    
                for (SecurityManagerListener listener : GeoServerExtensions
                        .extensions(SecurityManagerListener.class)) {
                    listener.handlePostChanged(securityContext);
                }
            } else {
                // Try to restore the original one
                if (Resources.exists(security) && !security.delete()) {
                    logValidationExceptions((ValidationResult) null,
                            new UnexpectedJobExecutionException(
                                    "Exception occurred while storing GeoServer security and services settings!",
                                    new IOException("It was not possible to cleanup the target security folder!")));
                }
                
                try {
                    Resources.copy(tmpDir, security);
                } catch (IOException e) {
                    logValidationExceptions((ValidationResult) null,
                            new UnexpectedJobExecutionException(
                                    "Exception occurred while storing GeoServer security and services settings!",
                                    new IOException("It was not possible to fully restore the original Security folder!", e)));
                }                
            }
        }

        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(backupFacade, "backupFacade must be set");
        Assert.notNull(getxStreamPersisterFactory(), "xstream must be set");
    }
}
