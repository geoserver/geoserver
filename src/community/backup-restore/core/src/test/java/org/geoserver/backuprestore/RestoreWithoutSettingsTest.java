package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

public class RestoreWithoutSettingsTest extends BackupRestoreTestSupport {

    protected static Backup backupFacade;

    @Before
    public void beforeTest() throws InterruptedException {
        backupFacade = (Backup) applicationContext.getBean("backupFacade");
        ensureCleanedQueues();

        // Authenticate as Administrator
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // add a workspace that shouldn't get removed if "purge" is set to false
        this.getTestData().addWorkspace("shouldNotBeDeleted", "http://snbd", getCatalog());
    }

    @Test
    public void testRestoreWithoutSettings() throws Exception {

        Map<String, String> params = new HashMap<>();
        params.put(Backup.PARAM_BEST_EFFORT_MODE, "true");
        params.put(Backup.PARAM_SKIP_SETTINGS, "true");
        params.put(Backup.PARAM_PURGE_RESOURCES, "false");

        assertNotNull(getCatalog().getWorkspaceByName("shouldNotBeDeleted"));

        RestoreExecutionAdapter restoreExecution =
                backupFacade.runRestoreAsync(
                        file("settings-modified-restore.zip"), null, null, null, params);

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

        if (restoreExecution.getStatus() != BatchStatus.COMPLETED
                && !restoreExecution.isRunning()) {
            backupFacade.stopExecution(restoreExecution.getId());
        }

        if (restoreExecution.getStatus() == BatchStatus.COMPLETED) {
            GeoServer geoServer = getGeoServer();
            assertEquals(null, geoServer.getLogging().getLocation());

            assertEquals(
                    "Andrea Aime",
                    geoServer.getGlobal().getSettings().getContact().getContactPerson());

            String configPasswordEncrypterName =
                    getSecurityManager().getSecurityConfig().getConfigPasswordEncrypterName();
            assertEquals("pbePasswordEncoder", configPasswordEncrypterName);

            Catalog catalog = geoServer.getCatalog();
            assertNotNull(catalog.getWorkspaceByName("shouldNotBeDeleted"));
        }
    }
}
