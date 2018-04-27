package org.geoserver.backuprestore;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSInfo;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

public class RestoreWithoutSettingsTest extends BackupRestoreTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        //add a workspace that shouldn't get removed if "purge" is set to false
        this.getTestData().addWorkspace("shouldNotBeDeleted", "http://snbd", getCatalog());
    }

    @Test
    public void testRestoreWithoutSettings() throws Exception {

        Map<String, String> params = new HashMap<>();
        params.put(Backup.PARAM_BEST_EFFORT_MODE, "true");
        params.put(Backup.PARAM_SKIP_SETTINGS, "true");
        params.put(Backup.PARAM_PURGE_RESOURCES, "false");

        assertNotNull(getCatalog().getWorkspaceByName("shouldNotBeDeleted"));

        RestoreExecutionAdapter restoreExecution = backupFacade
            .runRestoreAsync(file("settings-modified-restore.zip"), null, params);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getRestoreExecutions());
        assertTrue(!backupFacade.getRestoreExecutions().isEmpty());

        assertNotNull(restoreExecution);

        Thread.sleep(100);

        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        assertNotNull(restoreCatalog);

        while (restoreExecution.getStatus() != BatchStatus.COMPLETED) {
            Thread.sleep(100);

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

        GeoServer geoServer = getGeoServer();
        assertEquals(null, geoServer.getLogging().getLocation());

        assertEquals("Andrea Aime", geoServer.getGlobal().getSettings()
            .getContact().getContactPerson());

        WMSInfo serviceInfo = getGeoServer().getService(WMSInfo.class);
        assertEquals(null, serviceInfo.getFees());

        String configPasswordEncrypterName = getSecurityManager().getSecurityConfig().getConfigPasswordEncrypterName();
        assertEquals("pbePasswordEncoder", configPasswordEncrypterName);

        Catalog catalog = geoServer.getCatalog();
        assertNotNull(catalog.getWorkspaceByName("shouldNotBeDeleted"));

    }
}
