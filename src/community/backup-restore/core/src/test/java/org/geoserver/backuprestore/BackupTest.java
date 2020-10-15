/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.springframework.batch.core.BatchStatus;

/** @author Alessio Fabiani, GeoSolutions */
public class BackupTest extends BackupRestoreTestSupport {

    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testRunSpringBatchBackupJob() throws Exception {
        Hints hints = new Hints(new HashMap(2));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                        Backup.PARAM_BEST_EFFORT_MODE));

        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(
                        Files.asResource(
                                File.createTempFile("testRunSpringBatchBackupJob", ".zip")),
                        true,
                        null,
                        null,
                        null,
                        hints);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getBackupExecutions());
        assertTrue(!backupFacade.getBackupExecutions().isEmpty());
        assertNotNull(backupExecution);

        int cnt = 0;
        while (cnt < 100
                && (backupExecution.getStatus() != BatchStatus.COMPLETED
                        || backupExecution.isRunning())) {
            Thread.sleep(100);
            cnt++;

            if (backupExecution.getStatus() == BatchStatus.ABANDONED
                    || backupExecution.getStatus() == BatchStatus.FAILED
                    || backupExecution.getStatus() == BatchStatus.UNKNOWN) {

                for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }

        assertEquals(backupExecution.getStatus(), BatchStatus.COMPLETED);
        assertThat(ContinuableHandler.getInvocationsCount() > 2, is(true));
        // check that generic listener was invoked for the backup job
        assertThat(GenericListener.getBackupAfterInvocations(), is(4));
        assertThat(GenericListener.getBackupBeforeInvocations(), is(4));
        assertThat(GenericListener.getRestoreAfterInvocations(), is(3));
        assertThat(GenericListener.getRestoreBeforeInvocations(), is(3));
    }

    @Test
    public void testTryToRunMultipleSpringBatchBackupJobs() throws Exception {
        Hints hints = new Hints(new HashMap(2));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                        Backup.PARAM_BEST_EFFORT_MODE));

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
                    e.getMessage(),
                    "Could not start a new Backup Job Execution since there are currently Running jobs.");
        }

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getBackupExecutions());
        assertTrue(!backupFacade.getBackupExecutions().isEmpty());
        assertEquals(backupFacade.getBackupRunningExecutions().size(), 1);

        BackupExecutionAdapter backupExecution = null;
        final Iterator<BackupExecutionAdapter> iterator =
                backupFacade.getBackupExecutions().values().iterator();
        while (iterator.hasNext()) {
            backupExecution = iterator.next();
        }

        assertNotNull(backupExecution);

        int cnt = 0;
        while (cnt < 100
                && (backupExecution.getStatus() != BatchStatus.COMPLETED
                        || !backupExecution.isRunning())) {
            Thread.sleep(100);
            cnt++;

            if (backupExecution.getStatus() == BatchStatus.ABANDONED
                    || backupExecution.getStatus() == BatchStatus.FAILED
                    || backupExecution.getStatus() == BatchStatus.UNKNOWN) {
                LOGGER.severe("backupExecution.getStatus() == " + (backupExecution.getStatus()));

                for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }

        assertEquals(backupExecution.getStatus(), BatchStatus.COMPLETED);
        assertThat(ContinuableHandler.getInvocationsCount() > 2, is(true));
    }

    @Test
    public void testRunSpringBatchRestoreJob() throws Exception {
        Hints hints = new Hints(new HashMap(2));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                        Backup.PARAM_BEST_EFFORT_MODE));

        RestoreExecutionAdapter restoreExecution =
                backupFacade.runRestoreAsync(
                        file("geoserver-full-backup.zip"), null, null, null, hints);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getRestoreExecutions());
        assertTrue(!backupFacade.getRestoreExecutions().isEmpty());

        assertNotNull(restoreExecution);

        Thread.sleep(100);

        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        assertNotNull(restoreCatalog);

        int cnt = 0;
        while (cnt < 100
                && (restoreExecution.getStatus() != BatchStatus.COMPLETED
                        || !restoreExecution.isRunning())) {
            Thread.sleep(100);
            cnt++;

            if (restoreExecution.getStatus() == BatchStatus.ABANDONED
                    || restoreExecution.getStatus() == BatchStatus.FAILED
                    || restoreExecution.getStatus() == BatchStatus.UNKNOWN) {

                for (Throwable exception : restoreExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }

        if (restoreExecution.getStatus() != BatchStatus.COMPLETED && restoreExecution.isRunning()) {
            backupFacade.stopExecution(restoreExecution.getId());
        }

        if (restoreCatalog.getWorkspaces().size() > 0) {
            assertEquals(
                    restoreCatalog.getWorkspaces().size(), restoreCatalog.getNamespaces().size());
            assertEquals(9, restoreCatalog.getDataStores().size(), 9);
            assertEquals(43, restoreCatalog.getResources(FeatureTypeInfo.class).size());
            assertEquals(4, restoreCatalog.getResources(CoverageInfo.class).size());
            assertEquals(35, restoreCatalog.getStyles().size());
            assertEquals(33, restoreCatalog.getLayers().size());
            assertEquals(3, restoreCatalog.getLayerGroups().size());
        }

        checkExtraPropertiesExists();
        if (restoreExecution.getStatus() == BatchStatus.COMPLETED) {
            assertThat(ContinuableHandler.getInvocationsCount() > 2, is(true));
            // check that generic listener was invoked for the backup job
            assertThat(GenericListener.getBackupAfterInvocations(), is(2));
            assertThat(GenericListener.getBackupBeforeInvocations(), is(2));
            assertThat(GenericListener.getRestoreAfterInvocations(), is(3));
            assertThat(GenericListener.getRestoreBeforeInvocations(), is(3));
        }
    }

    @Test
    public void testParameterizedRestore() throws Exception {
        Hints hints = new Hints(new HashMap(2));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                        Backup.PARAM_BEST_EFFORT_MODE));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_PARAMETERIZE_PASSWDS),
                        Backup.PARAM_PARAMETERIZE_PASSWDS));

        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_PASSWORD_TOKENS, "*"),
                        "${sf:sf.passwd.encryptedValue}=foo"));

        RestoreExecutionAdapter restoreExecution =
                backupFacade.runRestoreAsync(
                        file("parameterized-restore.zip"), null, null, null, hints);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getRestoreExecutions());
        assertTrue(!backupFacade.getRestoreExecutions().isEmpty());

        assertNotNull(restoreExecution);

        Thread.sleep(100);

        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        assertNotNull(restoreCatalog);

        int cnt = 0;
        while (cnt < 100
                && (restoreExecution.getStatus() != BatchStatus.COMPLETED
                        || !restoreExecution.isRunning())) {
            Thread.sleep(100);
            cnt++;

            if (restoreExecution.getStatus() == BatchStatus.ABANDONED
                    || restoreExecution.getStatus() == BatchStatus.FAILED
                    || restoreExecution.getStatus() == BatchStatus.UNKNOWN) {

                for (Throwable exception : restoreExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }

        if (restoreExecution.getStatus() != BatchStatus.COMPLETED && restoreExecution.isRunning()) {
            backupFacade.stopExecution(restoreExecution.getId());
        }

        if (restoreCatalog.getWorkspaces().size() > 0) {
            assertEquals(
                    restoreCatalog.getWorkspaces().size(), restoreCatalog.getNamespaces().size());
            assertEquals(9, restoreCatalog.getDataStores().size());
            assertEquals(47, restoreCatalog.getResources(FeatureTypeInfo.class).size());
            assertEquals(4, restoreCatalog.getResources(CoverageInfo.class).size());
            assertEquals(35, restoreCatalog.getStyles().size());
            assertEquals(33, restoreCatalog.getLayers().size());
            assertEquals(3, restoreCatalog.getLayerGroups().size());
        }

        checkExtraPropertiesExists();
        if (restoreExecution.getStatus() == BatchStatus.COMPLETED) {
            assertThat(ContinuableHandler.getInvocationsCount() > 2, is(true));
            // check that generic listener was invoked for the backup job
            assertThat(GenericListener.getBackupAfterInvocations(), is(0));
            assertThat(GenericListener.getBackupBeforeInvocations(), is(0));
            assertThat(GenericListener.getRestoreAfterInvocations(), is(1));
            assertThat(GenericListener.getRestoreBeforeInvocations(), is(1));

            DataStoreInfo restoredDataStore =
                    restoreCatalog.getStoreByName("sf", "sf", DataStoreInfo.class);
            Serializable passwd = restoredDataStore.getConnectionParameters().get("passwd");
            assertEquals("foo", passwd);
        }
    }

    @Test
    public void testRunSpringBatchFilteredRestoreJob() throws Exception {
        Hints hints = new Hints(new HashMap(2));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                        Backup.PARAM_BEST_EFFORT_MODE));

        Filter filter = ECQL.toFilter("name = 'topp'");
        RestoreExecutionAdapter restoreExecution =
                backupFacade.runRestoreAsync(
                        file("geoserver-full-backup.zip"), filter, null, null, hints);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getRestoreExecutions());
        assertTrue(!backupFacade.getRestoreExecutions().isEmpty());

        assertNotNull(restoreExecution);

        Thread.sleep(100);

        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        assertNotNull(restoreCatalog);

        int cnt = 0;
        while (cnt < 100 && (restoreExecution.getStatus() != BatchStatus.COMPLETED)) {
            Thread.sleep(100);
            cnt++;

            if (restoreExecution.getStatus() == BatchStatus.ABANDONED
                    || restoreExecution.getStatus() == BatchStatus.FAILED
                    || restoreExecution.getStatus() == BatchStatus.UNKNOWN) {

                for (Throwable exception : restoreExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }

        assertEquals(restoreExecution.getStatus(), BatchStatus.COMPLETED);
        if (restoreCatalog.getWorkspaces().size() > 0) {
            assertEquals(9, restoreCatalog.getDataStores().size());
            assertEquals(35, restoreCatalog.getStyles().size());
        }

        checkExtraPropertiesExists();
        assertThat(ContinuableHandler.getInvocationsCount() > 2, is(true));
    }

    @Test
    public void testStopSpringBatchBackupJob() throws Exception {
        Hints hints = new Hints(new HashMap(2));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                        Backup.PARAM_BEST_EFFORT_MODE));

        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(
                        Files.asResource(
                                File.createTempFile("testRunSpringBatchBackupJob", ".zip")),
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
                    exception.printStackTrace();
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
                        LOGGER.log(
                                Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                        exception.printStackTrace();
                    }
                    break;
                }
            }

            assertEquals(backupExecution.getStatus(), BatchStatus.STOPPED);
        }
    }

    @Test
    public void testBackupExcludedResources() throws Exception {
        GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();

        BackupUtils.dir(dd.get(Paths.BASE), "/foo/folder");
        assertTrue(Resources.exists(dd.get("/foo/folder")));

        Hints hints = new Hints(new HashMap(2));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE),
                        Backup.PARAM_BEST_EFFORT_MODE));
        hints.add(
                new Hints(
                        new Hints.OptionKey(Backup.PARAM_EXCLUDE_FILE_PATH, "*"),
                        "/demo;/layergroups;/cite;/WEB-INF;/foo/folder"));

        Resource backupFile =
                Files.asResource(
                        File.createTempFile("testRunSpringBatchBackupJobFiltered", ".zip"));
        if (Resources.exists(backupFile)) {
            assertTrue(backupFile.delete());
        }
        BackupExecutionAdapter backupExecution =
                backupFacade.runBackupAsync(backupFile, true, null, null, null, hints);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getBackupExecutions());
        assertTrue(!backupFacade.getBackupExecutions().isEmpty());
        assertNotNull(backupExecution);

        int cnt = 0;
        while (cnt < 100
                && (backupExecution.getStatus() != BatchStatus.COMPLETED
                        || backupExecution.isRunning())) {
            Thread.sleep(100);
            cnt++;

            if (backupExecution.getStatus() == BatchStatus.ABANDONED
                    || backupExecution.getStatus() == BatchStatus.FAILED
                    || backupExecution.getStatus() == BatchStatus.UNKNOWN) {

                for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }

        assertEquals(backupExecution.getStatus(), BatchStatus.COMPLETED);

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

    /**
     * Helper method that just check if the extra properties file was correctly backup / restore.
     */
    private void checkExtraPropertiesExists() {
        // find the properties file on the current data dir
        GeoServerDataDirectory dataDirectory =
                GeoServerExtensions.bean(GeoServerDataDirectory.class);
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
}
