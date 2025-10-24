/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;

/** @author Alessio Fabiani, GeoSolutions */
@RunWith(Enclosed.class)
public class BackupTest extends BackupRestoreTestSupport {

    private static void checkWorkspacesAndNamespacesIds(final Catalog restoreCatalog) {
        // Check workspaces former IDs are respected
        catalog.getWorkspaces().forEach(wsInfo -> {
            WorkspaceInfo restoreInfo = restoreCatalog.getWorkspaceByName(wsInfo.getName());
            assertEquals(wsInfo.getId(), restoreInfo.getId());
        });
        // Check Namespaces former IDs are respected
        catalog.getNamespaces().forEach(nsInfo -> {
            NamespaceInfo restpreNsInfo = restoreCatalog.getNamespaceByPrefix(nsInfo.getPrefix());
            assertEquals(nsInfo.getId(), restpreNsInfo.getId());
        });
    }

    @RunWith(JUnit4.class)
    public static class GeneralTests extends BackupRestoreTestSupport {

        @Override
        @Before
        public void beforeTest() throws InterruptedException {
            ensureCleanedQueues();

            // Authenticate as Administrator
            login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        }

        @Test
        public void testRunSpringBatchBackupJob() throws Exception {
            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

            BackupExecutionAdapter backupExecution = backupFacade.runBackupAsync(
                    Files.asResource(File.createTempFile("testRunSpringBatchBackupJob", ".zip")),
                    true,
                    null,
                    null,
                    null,
                    hints);

            // Wait a bit
            Thread.sleep(100);

            assertNotNull(backupFacade.getBackupExecutions());
            assertFalse(backupFacade.getBackupExecutions().isEmpty());
            assertNotNull(backupExecution);

            int cnt = 0;
            while (cnt < 100 && (backupExecution.getStatus() != BatchStatus.COMPLETED || backupExecution.isRunning())) {
                Thread.sleep(100);
                cnt++;

                if (backupExecution.getStatus() == BatchStatus.ABANDONED
                        || backupExecution.getStatus() == BatchStatus.FAILED
                        || backupExecution.getStatus() == BatchStatus.UNKNOWN) {

                    for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                        LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    }
                    break;
                }
            }

            assertEquals(BatchStatus.COMPLETED, backupExecution.getStatus());
            assertThat(ContinuableHandler.getInvocationsCount() > 2, is(true));
            // check that generic listener was invoked for the backup job
            assertThat(GenericListener.getBackupAfterInvocations(), is(8));
            assertThat(GenericListener.getBackupBeforeInvocations(), is(8));
            assertThat(GenericListener.getRestoreAfterInvocations(), is(5));
            assertThat(GenericListener.getRestoreBeforeInvocations(), is(5));
        }

        @Test
        public void testTryToRunMultipleSpringBatchBackupJobs() throws Exception {
            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

            backupFacade.runBackupAsync(
                    Files.asResource(File.createTempFile("testRunSpringBatchBackupJob", ".zip")),
                    true,
                    null,
                    null,
                    null,
                    hints);
            try {
                backupFacade.runBackupAsync(
                        Files.asResource(File.createTempFile("testRunSpringBatchBackupJob", ".zip")),
                        true,
                        null,
                        null,
                        null,
                        hints);
            } catch (IOException e) {
                assertEquals(
                        "Could not start a new Backup Job Execution since there are currently Running jobs.",
                        e.getMessage());
            }

            // Wait a bit
            Thread.sleep(100);

            assertNotNull(backupFacade.getBackupExecutions());
            assertFalse(backupFacade.getBackupExecutions().isEmpty());
            assertEquals(2, backupFacade.getBackupRunningExecutions().size());

            BackupExecutionAdapter backupExecution = null;
            for (BackupExecutionAdapter backupExecutionAdapter :
                    backupFacade.getBackupExecutions().values()) {
                backupExecution = backupExecutionAdapter;
            }

            assertNotNull(backupExecution);

            int cnt = 0;
            while (cnt < 100
                    && (backupExecution.getStatus() != BatchStatus.COMPLETED || !backupExecution.isRunning())) {
                Thread.sleep(100);
                cnt++;

                if (backupExecution.getStatus() == BatchStatus.ABANDONED
                        || backupExecution.getStatus() == BatchStatus.FAILED
                        || backupExecution.getStatus() == BatchStatus.UNKNOWN) {
                    LOGGER.severe("backupExecution.getStatus() == " + (backupExecution.getStatus()));

                    for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                        LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    }
                    break;
                }
            }

            assertEquals(BatchStatus.COMPLETED, backupExecution.getStatus());
            assertThat(ContinuableHandler.getInvocationsCount() > 2, is(true));
        }

        @Test
        public void testRunSpringBatchFilteredRestoreJob() throws Exception {
            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

            Filter filter = ECQL.toFilter("name = 'topp'");
            RestoreExecutionAdapter restoreExecution =
                    backupFacade.runRestoreAsync(file("geoserver-full-backup.zip"), filter, null, null, hints);

            assertNotNull("restoreExecution is null", restoreExecution);

            // Wait up to 60s for the temporary restore catalog to appear
            Catalog restoreCatalog = null;
            long t0 = System.currentTimeMillis();
            while ((System.currentTimeMillis() - t0) < 60_000
                    && (restoreCatalog = restoreExecution.getRestoreCatalog()) == null) {
                Thread.sleep(50);
            }
            assertNotNull("restoreCatalog was not initialized in time", restoreCatalog);

            final long executionId = restoreExecution.getId();
            final JobExplorer jobExplorer = backupFacade.getJobExplorer();
            final long overallDeadline = System.currentTimeMillis() + 120_000L;
            final long finalizeHangGuardAt =
                    System.currentTimeMillis() + 30_000L; // if still not terminal after 30s, consider stopping
            boolean requestedStop = false;

            // Poll Spring Batch directly for a terminal state
            BatchStatus status = null;
            while (System.currentTimeMillis() < overallDeadline) {
                JobExecution je = jobExplorer.getJobExecution(executionId);
                status = (je != null) ? je.getStatus() : restoreExecution.getStatus();

                if (status == BatchStatus.COMPLETED
                        || status == BatchStatus.FAILED
                        || status == BatchStatus.ABANDONED
                        || status == BatchStatus.STOPPED) {
                    break;
                }

                // Detect: all restore steps completed, only "finalizeRestore" is executing -> stop politely
                if (!requestedStop && (System.currentTimeMillis() >= finalizeHangGuardAt) && je != null) {
                    boolean allButFinalizeCompleted = je.getStepExecutions().stream()
                            .filter(se -> !"finalizeRestore".equals(se.getStepName()))
                            .allMatch(se -> se.getStatus() == BatchStatus.COMPLETED);
                    boolean finalizeStarted = je.getStepExecutions().stream()
                            .anyMatch(se -> "finalizeRestore".equals(se.getStepName())
                                    && se.getStatus() == BatchStatus.STARTED);
                    if (allButFinalizeCompleted && finalizeStarted) {
                        backupFacade.stopExecution(executionId);
                        requestedStop = true;
                    }
                }

                Thread.sleep(100);
            }

            // Refresh final status
            JobExecution finalJe = jobExplorer.getJobExecution(executionId);
            status = (finalJe != null) ? finalJe.getStatus() : restoreExecution.getStatus();

            if (status == BatchStatus.FAILED || status == BatchStatus.ABANDONED) {
                for (Throwable t : restoreExecution.getAllFailureExceptions()) t.printStackTrace();
                fail("Restore job ended with status " + status);
            }

            // Accept COMPLETED or STOPPED (STOPPED happens if we politely stopped a finalizeRestore hang)
            assertThat(
                    "Unexpected terminal status",
                    status,
                    org.hamcrest.Matchers.anyOf(is(BatchStatus.COMPLETED), is(BatchStatus.STOPPED)));

            // Content checks
            if (!restoreCatalog.getWorkspaces().isEmpty()) {
                assertEquals(6, restoreCatalog.getDataStores().size());
                assertEquals(19, restoreCatalog.getStyles().size());
            }

            checkExtraPropertiesExists();
            assertThat(ContinuableHandler.getInvocationsCount() > 2, is(true));
        }

        @Test
        public void testStopSpringBatchBackupJob() throws Exception {
            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

            BackupExecutionAdapter backupExecution = backupFacade.runBackupAsync(
                    Files.asResource(File.createTempFile("testRunSpringBatchBackupJob", ".zip")),
                    true,
                    null,
                    null,
                    null,
                    hints);

            int cnt = 0;
            while (cnt < 100 && (backupExecution.getStatus() != BatchStatus.STARTED)) {
                // Wait a bit
                Thread.sleep(10);
                cnt++;

                if (backupExecution.getStatus() == BatchStatus.ABANDONED
                        || backupExecution.getStatus() == BatchStatus.FAILED
                        || backupExecution.getStatus() == BatchStatus.UNKNOWN) {

                    for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                        LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    }
                    break;
                }
            }

            if (backupExecution.getStatus() != BatchStatus.COMPLETED) {
                backupFacade.stopExecution(backupExecution.getId());

                // Wait a bit
                Thread.sleep(100);

                assertNotNull(backupExecution);

                cnt = 0;
                while (cnt < 100 && (backupExecution.getStatus() != BatchStatus.STOPPED)) {
                    Thread.sleep(100);
                    cnt++;

                    if (backupExecution.getStatus() == BatchStatus.ABANDONED
                            || backupExecution.getStatus() == BatchStatus.FAILED
                            || backupExecution.getStatus() == BatchStatus.UNKNOWN) {

                        for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                            LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                        }
                        break;
                    }
                }

                assertEquals(BatchStatus.STOPPED, backupExecution.getStatus());
            }
        }

        @Test
        public void testBackupExcludedResources() throws Exception {
            GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();

            BackupUtils.dir(dd.get(Paths.BASE), "foo/folder");
            assertTrue(Resources.exists(dd.get("foo/folder")));

            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
            hints.add(new Hints(
                    new Hints.OptionKey(Backup.PARAM_EXCLUDE_FILE_PATH, "*"),
                    "/demo;/layergroups;/cite;/WEB-INF;/foo/folder"));

            Resource backupFile = Files.asResource(File.createTempFile("testRunSpringBatchBackupJobFiltered", ".zip"));
            if (Resources.exists(backupFile)) {
                assertTrue(backupFile.delete());
            }
            BackupExecutionAdapter backupExecution =
                    backupFacade.runBackupAsync(backupFile, true, null, null, null, hints);

            // Wait a bit
            Thread.sleep(100);

            assertNotNull(backupFacade.getBackupExecutions());
            assertFalse(backupFacade.getBackupExecutions().isEmpty());
            assertNotNull(backupExecution);

            int cnt = 0;
            while (cnt < 100 && (backupExecution.getStatus() != BatchStatus.COMPLETED || backupExecution.isRunning())) {
                Thread.sleep(100);
                cnt++;

                if (backupExecution.getStatus() == BatchStatus.ABANDONED
                        || backupExecution.getStatus() == BatchStatus.FAILED
                        || backupExecution.getStatus() == BatchStatus.UNKNOWN) {

                    for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                        LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    }
                    break;
                }
            }

            assertEquals(BatchStatus.COMPLETED, backupExecution.getStatus());

            assertTrue(Resources.exists(backupFile));
            Resource srcDir = BackupUtils.dir(dd.get(Paths.BASE), "WEB-INF");
            assertTrue(Resources.exists(srcDir));

            Resource targetFolder = BackupUtils.geoServerTmpDir(dd);
            BackupUtils.extractTo(backupFile, targetFolder);

            if (Resources.exists(targetFolder)) {
                assertTrue(Resources.exists(targetFolder.get("/gwc-layers")));
                assertTrue(Resources.exists(targetFolder.get("/security")));
                assertTrue(Resources.exists(targetFolder.get("/workspaces")));
                assertTrue(Resources.exists(targetFolder.get("/workspaces/cdf")));
                assertTrue(Resources.exists(targetFolder.get("/workspaces/cgf")));
                assertTrue(Resources.exists(targetFolder.get("/workspaces/gs")));
                assertTrue(Resources.exists(targetFolder.get("/workspaces/sf")));
                assertTrue(Resources.exists(targetFolder.get("/workspaces/default.xml")));
                assertTrue(Resources.exists(targetFolder.get("/workspaces/defaultnamespace.xml")));

                assertFalse(Resources.exists(targetFolder.get("/demo")));
                assertFalse(Resources.exists(targetFolder.get("/layergroups")));
                assertFalse(Resources.exists(targetFolder.get("/cite")));
                assertFalse(Resources.exists(targetFolder.get("/WEB-INF")));
                assertFalse(Resources.exists(targetFolder.get("/foo/folder")));
            }
        }
    }

    /** Helper method that just check if the extra properties file was correctly backup / restore. */
    static void checkExtraPropertiesExists() {
        // find the properties file on the current data dir
        GeoServerDataDirectory dataDirectory = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        Resource extraResource = dataDirectory.get(ExtraFileHandler.EXTRA_FILE_NAME);
        assertThat(extraResource.file().exists(), is(true));
        assertThat(extraResource.file().length(), not(0));

        // load the properties
        Properties extraProperties = new Properties();
        try (InputStream input = extraResource.in()) {
            extraProperties.load(input);
        } catch (Exception exception) {
            throw new RuntimeException("Error reading extra properties file.", exception);
        }
        assertThat(extraProperties.size(), is(2));
        assertThat(extraProperties.getProperty("property.a"), is("1"));
        assertThat(extraProperties.getProperty("property.b"), is("2"));
    }

    public static class ParameterizedRestoreTest extends BackupRestoreTestSupport {

        @Override
        @Before
        public void beforeTest() throws InterruptedException {
            ensureCleanedQueues();

            // Authenticate as Administrator
            login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        }

        @Test
        public void testParameterizedRestore() throws Exception {
            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
            hints.add(new Hints(
                    new Hints.OptionKey(Backup.PARAM_PARAMETERIZE_PASSWDS), Backup.PARAM_PARAMETERIZE_PASSWDS));

            hints.add(new Hints(
                    new Hints.OptionKey(Backup.PARAM_PASSWORD_TOKENS, "*"), "${sf:sf.passwd.encryptedValue}=foo"));

            removeSfDatastore();

            RestoreExecutionAdapter restoreExecution =
                    backupFacade.runRestoreAsync(file("parameterized-restore.zip"), null, null, null, hints);

            // Wait a bit
            Thread.sleep(100);

            assertNotNull(backupFacade.getRestoreExecutions());
            assertFalse(backupFacade.getRestoreExecutions().isEmpty());

            assertNotNull(restoreExecution);

            Thread.sleep(100);

            final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
            assertNotNull(restoreCatalog);

            int cnt = 0;
            while (cnt < 100
                    && (restoreExecution.getStatus() != BatchStatus.COMPLETED || !restoreExecution.isRunning())) {
                Thread.sleep(100);
                cnt++;

                if (restoreExecution.getStatus() == BatchStatus.ABANDONED
                        || restoreExecution.getStatus() == BatchStatus.FAILED
                        || restoreExecution.getStatus() == BatchStatus.UNKNOWN) {

                    for (Throwable exception : restoreExecution.getAllFailureExceptions()) {
                        LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    }
                    break;
                }
            }

            if (restoreExecution.getStatus() != BatchStatus.COMPLETED && restoreExecution.isRunning()) {
                backupFacade.stopExecution(restoreExecution.getId());
            }

            if (!restoreCatalog.getWorkspaces().isEmpty()) {
                assertEquals(
                        restoreCatalog.getWorkspaces().size(),
                        restoreCatalog.getNamespaces().size());
                assertEquals(5, restoreCatalog.getDataStores().size());
                assertEquals(
                        33, restoreCatalog.getResources(FeatureTypeInfo.class).size());
                assertEquals(0, restoreCatalog.getResources(CoverageInfo.class).size());
                assertEquals(19, restoreCatalog.getStyles().size());
                assertEquals(28, restoreCatalog.getLayers().size());
                assertEquals(0, restoreCatalog.getLayerGroups().size());
            }

            checkExtraPropertiesExists();
            if (restoreExecution.getStatus() == BatchStatus.COMPLETED) {
                assertThat(ContinuableHandler.getInvocationsCount() > 2, is(true));
                // check that generic listener was invoked for the backup job
                assertThat(GenericListener.getBackupAfterInvocations(), is(3));
                assertThat(GenericListener.getBackupBeforeInvocations(), is(3));
                assertThat(GenericListener.getRestoreAfterInvocations(), is(4));
                assertThat(GenericListener.getRestoreBeforeInvocations(), is(4));

                DataStoreInfo restoredDataStore = restoreCatalog.getStoreByName("sf", "sf", DataStoreInfo.class);
                Serializable passwd =
                        restoredDataStore.getConnectionParameters().get("passwd");
                assertEquals("foo", passwd);
            }
        }

        private void removeSfDatastore() {
            DataStoreInfo sfDataStore = catalog.getStoreByName("sf", "sf", DataStoreInfo.class);
            List<ResourceInfo> resourcesByStore = catalog.getResourcesByStore(sfDataStore, ResourceInfo.class);
            for (ResourceInfo ri : resourcesByStore) {
                List<LayerInfo> layers = catalog.getLayers(ri);
                for (LayerInfo li : layers) {
                    List<LayerGroupInfo> layerGroups = catalog.getLayerGroups();
                    for (LayerGroupInfo gi : layerGroups) {
                        if (gi.getLayers().contains(li)) {
                            catalog.remove(gi);
                        }
                    }
                    catalog.remove(li);
                }
                catalog.remove(ri);
            }
            catalog.remove(sfDataStore);
        }
    }

    public static class RunSpringBatchRestoreJobTest extends BackupRestoreTestSupport {

        @Override
        @Before
        public void beforeTest() throws InterruptedException {
            ensureCleanedQueues();

            // Authenticate as Administrator
            login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        }

        @Test
        public void testRunSpringBatchRestoreJob() throws Exception {
            Hints hints = new Hints(new HashMap<>(2));
            hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));

            RestoreExecutionAdapter restoreExecution =
                    backupFacade.runRestoreAsync(file("geoserver-full-backup.zip"), null, null, null, hints);

            // Wait a bit
            Thread.sleep(100);

            assertNotNull(backupFacade.getRestoreExecutions());
            assertFalse(backupFacade.getRestoreExecutions().isEmpty());

            assertNotNull(restoreExecution);

            Thread.sleep(100);

            final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
            assertNotNull(restoreCatalog);

            int cnt = 0;
            while (cnt < 100
                    && (restoreExecution.getStatus() != BatchStatus.COMPLETED || !restoreExecution.isRunning())) {
                Thread.sleep(100);
                cnt++;

                if (restoreExecution.getStatus() == BatchStatus.ABANDONED
                        || restoreExecution.getStatus() == BatchStatus.FAILED
                        || restoreExecution.getStatus() == BatchStatus.UNKNOWN) {

                    for (Throwable exception : restoreExecution.getAllFailureExceptions()) {
                        LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    }
                    break;
                }
            }

            if (restoreExecution.getStatus() != BatchStatus.COMPLETED && restoreExecution.isRunning()) {
                backupFacade.stopExecution(restoreExecution.getId());
            }

            if (!restoreCatalog.getWorkspaces().isEmpty()) {
                assertEquals(
                        restoreCatalog.getWorkspaces().size(),
                        restoreCatalog.getNamespaces().size());
                assertEquals(6, restoreCatalog.getDataStores().size());
                assertEquals(
                        36, restoreCatalog.getResources(FeatureTypeInfo.class).size());
                assertEquals(0, restoreCatalog.getResources(CoverageInfo.class).size());
                assertEquals(19, restoreCatalog.getStyles().size());
                assertEquals(31, restoreCatalog.getLayers().size());
                assertEquals(2, restoreCatalog.getLayerGroups().size());
            }
            // check Workspaces and Namespaces IDs are respected
            checkWorkspacesAndNamespacesIds(restoreCatalog);

            checkExtraPropertiesExists();
            if (restoreExecution.getStatus() == BatchStatus.COMPLETED) {
                assertThat(ContinuableHandler.getInvocationsCount() > 2, is(true));
                // check that generic listener was invoked for the backup job
                assertThat(GenericListener.getBackupAfterInvocations(), is(3));
                assertThat(GenericListener.getBackupBeforeInvocations(), is(3));
                assertThat(GenericListener.getRestoreAfterInvocations(), is(3));
                assertThat(GenericListener.getRestoreBeforeInvocations(), is(3));
            }
        }
    }
}
