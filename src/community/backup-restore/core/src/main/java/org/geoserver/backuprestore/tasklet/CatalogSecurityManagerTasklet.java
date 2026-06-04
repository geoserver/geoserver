/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import java.io.IOException;
import java.util.logging.Level;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.SecurityMerger;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecurityManagerListener;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.util.Assert;

/**
 * Concrete implementation of the {@link AbstractCatalogBackupRestoreTasklet}. <br>
 * Actually takes care of dumping/restoring GeoServer Security subsystem.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogSecurityManagerTasklet extends AbstractCatalogBackupRestoreTasklet {

    public static final String SECURITY_RESOURCE_NAME = "security";
    private boolean skipSecuritySettings = false;

    public CatalogSecurityManagerTasklet(Backup backupFacade) {
        super(backupFacade);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        boolean skipSettings = Backup.isSkipSettings(stepExecution.getJobParameters());
        boolean skipSecurity = Backup.isSkipSecuritySettings(stepExecution.getJobParameters());

        this.skipSecuritySettings = skipSettings || skipSecurity || filterIsValid();
    }

    @Override
    RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext, JobExecution jobExecution)
            throws Exception {
        final GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();

        // GeoServer Security Folder
        // dd.getResourceStore().get(SECURITY_RESOURCE_NAME);
        final Resource security = dd.getSecurity(Paths.BASE);

        if (!isNew() && !skipSecuritySettings) {
            /*
             * BACKUP Security Resources
             */
            final String outputFolderURL = jobExecution.getJobParameters().getString(Backup.PARAM_OUTPUT_FILE_PATH);
            final Resource targetBackupFolder = Resources.fromURL(outputFolderURL);
            final Resource securityTargetResource = BackupUtils.dir(targetBackupFolder, SECURITY_RESOURCE_NAME);

            // Copy the Security files into the destination resource
            try {
                Resources.copy(security, securityTargetResource);
            } catch (IOException e) {
                logValidationExceptions(
                        (ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!", e));
            }

            // Test that the security folder has been correctly saved
            GeoServerSecurityManager testGssm = null;
            try {
                testGssm = new GeoServerSecurityManager(new GeoServerDataDirectory(targetBackupFolder.dir()));
                testGssm.setApplicationContext(Backup.getContext());
                testGssm.reload();

                // TODO: Perform validation tests here using "testGssm"

                // TODO: Save warnings and validation issues on the JobContext
            } catch (Exception e) {
                logValidationExceptions(
                        (ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!", e));
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
             * <p>Try to load the configuration from there and if everything is ok: 1. Replace the security folders 2.
             * Destroy and reload the appContext GeoServerSecurityManager 3. Issue SecurityManagerListener extensions
             * handlePostChanged(...)
             */
            final String inputFolderURL = jobExecution.getJobParameters().getString(Backup.PARAM_INPUT_FILE_PATH);
            final Resource sourceRestoreFolder = Resources.fromURL(inputFolderURL);
            final Resource sourceSecurityResource = BackupUtils.dir(sourceRestoreFolder, SECURITY_RESOURCE_NAME);

            // A backup taken with a workspace filter (or with skip-security) does not include the security folder.
            // In that case the restore must NOT touch the target's security: deleting it (below) and copying an empty
            // source would leave the instance with no user/group services, and the subsequent reload fails with
            // "User/group service default does not exist". Detect the missing security config and skip instead, so a
            // partial / cross-instance restore leaves the target's existing security settings intact.
            if (!Resources.exists(sourceSecurityResource.get("config.xml"))) {
                LOGGER.warning("The backup archive contains no security configuration (no security/config.xml); "
                        + "skipping security restore and leaving the target's security settings untouched.");
                return RepeatStatus.FINISHED;
            }

            // BK_MERGE_SECURITY: instead of replacing the target's whole security folder (which would carry the
            // source-encrypted keystore, unreadable on a target with a different master password), merge the archive's
            // users / groups / roles into the target's existing services, keeping the target's configuration, keystore
            // and master password. This is the migration-safe path across instances.
            if (Backup.isMergeSecurity(jobExecution.getJobParameters())) {
                if (isDryRun()) {
                    LOGGER.info("Dry-run: BK_MERGE_SECURITY restore would merge the archive's users/groups/roles into "
                            + "the target's existing security services.");
                    return RepeatStatus.FINISHED;
                }
                GeoServerSecurityManager sourceGssm = null;
                try {
                    sourceGssm = new GeoServerSecurityManager(
                            new GeoServerDataDirectory(new GeoServerResourceLoader(sourceRestoreFolder.dir())));
                    sourceGssm.setApplicationContext(Backup.getContext());
                    sourceGssm.reload();

                    GeoServerSecurityManager targetGssm = GeoServerExtensions.bean(GeoServerSecurityManager.class);
                    SecurityMerger merger = new SecurityMerger(targetGssm);
                    merger.merge(sourceGssm);
                    for (String warning : merger.getWarnings()) {
                        LOGGER.warning("Security merge: " + warning);
                    }
                    targetGssm.reload();
                    for (SecurityManagerListener listener :
                            GeoServerExtensions.extensions(SecurityManagerListener.class)) {
                        listener.handlePostChanged(targetGssm);
                    }
                    LOGGER.info("BK_MERGE_SECURITY restore added " + merger.getUsersAdded() + " users, "
                            + merger.getGroupsAdded() + " groups and " + merger.getRolesAdded()
                            + " roles into the target's existing security services.");
                } catch (Exception e) {
                    logValidationExceptions(
                            (ValidationResult) null,
                            new UnexpectedJobExecutionException(
                                    "Exception occurred while merging GeoServer security settings!", e));
                } finally {
                    if (sourceGssm != null) {
                        try {
                            sourceGssm.destroy();
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Source GeoServerSecurityManager destroy error!", e);
                        }
                    }
                }
                return RepeatStatus.FINISHED;
            }

            // Test that the security folder has been correctly saved
            GeoServerSecurityManager testGssm = null;
            try {
                testGssm = new GeoServerSecurityManager(
                        new GeoServerDataDirectory(new GeoServerResourceLoader(sourceRestoreFolder.dir())));
                testGssm.setApplicationContext(Backup.getContext());
                testGssm.reload();

                // TODO: Perform more validation tests here using "testGssm"

                // TODO: Save detailed warnings and validation issues on the JobContext
            } catch (Exception e) {
                logValidationExceptions(
                        (ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!", e));
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
                logValidationExceptions(
                        (ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!",
                                new IOException("It was not possible to backup the original Security folder!", e)));
            }

            if (Resources.exists(security) && !security.delete()) {
                // Try to restore the original one
                try {
                    Resources.copy(tmpDir, security);
                } catch (IOException e) {
                    logValidationExceptions(
                            (ValidationResult) null,
                            new UnexpectedJobExecutionException(
                                    "Exception occurred while storing GeoServer security and services settings!",
                                    new IOException(
                                            "It was not possible to fully restore the original Security folder!", e)));
                }

                logValidationExceptions(
                        (ValidationResult) null,
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
                        logValidationExceptions(
                                (ValidationResult) null,
                                new UnexpectedJobExecutionException(
                                        "Exception occurred while storing GeoServer security and services settings!",
                                        new IOException(
                                                "It was not possible to fully restore the original Security folder!",
                                                e1)));
                    }

                    logValidationExceptions(
                            (ValidationResult) null,
                            new UnexpectedJobExecutionException(
                                    "Exception occurred while storing GeoServer security and services settings!", e));
                }

                // Reload Security Context
                GeoServerSecurityManager securityContext = GeoServerExtensions.bean(GeoServerSecurityManager.class);
                securityContext.reload();

                for (SecurityManagerListener listener : GeoServerExtensions.extensions(SecurityManagerListener.class)) {
                    listener.handlePostChanged(securityContext);
                }
            } else {
                // Try to restore the original one
                if (Resources.exists(security) && !security.delete()) {
                    logValidationExceptions(
                            (ValidationResult) null,
                            new UnexpectedJobExecutionException(
                                    "Exception occurred while storing GeoServer security and services settings!",
                                    new IOException("It was not possible to cleanup the target security folder!")));
                }

                try {
                    Resources.copy(tmpDir, security);
                } catch (IOException e) {
                    logValidationExceptions(
                            (ValidationResult) null,
                            new UnexpectedJobExecutionException(
                                    "Exception occurred while storing GeoServer security and services settings!",
                                    new IOException(
                                            "It was not possible to fully restore the original Security folder!", e)));
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
