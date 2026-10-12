/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.platform.resource.Resources;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

/**
 * A full backup kept inside the data directory, then a full (purge) restore of it, checked the way the restore leaves
 * things: on disk, and in the catalog once it is reloaded from the data directory.
 */
public class FullRestoreRoundTripTest extends BackupRestoreTestSupport {

    @Override
    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testFullRestoreRoundTrip() throws Exception {
        GeoServerDataDirectory dd = getDataDirectory();
        assertNotNull("the test catalog has a workspace layer group", catalog.getLayerGroupByName("sf", "local"));
        File gwcConfig = dd.get("gwc/geowebcache.xml").file();
        Assume.assumeTrue("GWC wrote its configuration", gwcConfig.length() > 0);
        byte[] gwcConfigBefore = Files.readAllBytes(gwcConfig.toPath());

        // A lock provider that really locks, as in a cluster: a stream left open by the backup keeps its lock.
        GeoServerInfo global = getGeoServer().getGlobal();
        String lockProvider = global.getLockProviderName();
        global.setLockProviderName("memoryLockProvider");
        getGeoServer().save(global);
        try {
            // the archive goes where an administrator keeps them, next to an older one
            File backups = dd.get("backups").dir();
            File older = new File(backups, "older.zip");
            Files.writeString(older.toPath(), "an older archive");
            File archive = new File(backups, "roundtrip.zip");

            Map<String, String> params = new HashMap<>();
            params.put(Backup.PARAM_BEST_EFFORT_MODE, "true");
            BackupExecutionAdapter backup = backupFacade.runBackupAsync(
                    org.geoserver.platform.resource.Files.asResource(archive), true, null, null, null, params);
            waitForCompletion(backup);
            assertEquals(BatchStatus.COMPLETED, backup.getStatus());

            try (ZipFile zip = new ZipFile(archive)) {
                assertNotNull(
                        "the archive carries the layer group", zip.getEntry("workspaces/sf/layergroups/local.xml"));
                assertNotNull("the archive carries the GWC configuration", zip.getEntry("gwc/geowebcache.xml"));
                assertFalse(
                        "the archive must not carry the folder it is written to",
                        zip.stream().anyMatch(e -> e.getName().startsWith("backups/")));
            }
            byte[] archived = Files.readAllBytes(archive.toPath());
            Files.writeString(older.toPath(), "an older archive, changed after the backup");

            params.put(Backup.PARAM_PURGE_RESOURCES, "true");
            RestoreExecutionAdapter restore = backupFacade.runRestoreAsync(
                    org.geoserver.platform.resource.Files.asResource(archive), null, null, null, params);
            waitForCompletion(restore);
            assertEquals(BatchStatus.COMPLETED, restore.getStatus());

            // the purge wiped the workspace folders: the layer group must have been written back
            assertTrue(
                    "a full restore must write the workspace layer group back",
                    Resources.exists(dd.get("workspaces/sf/layergroups/local.xml")));
            // the restore reloads the catalog after the job; reload again so the check does not race with it
            getGeoServer().reload();
            assertNotNull("the layer group after the reload", catalog.getLayerGroupByName("sf", "local"));

            assertTrue("the restore must put the GWC configuration back", gwcConfig.isFile());
            assertArrayEquals("as it was", gwcConfigBefore, Files.readAllBytes(gwcConfig.toPath()));
            assertArrayEquals("the restore leaves its archive alone", archived, Files.readAllBytes(archive.toPath()));
            assertEquals(
                    "and the other archives next to it",
                    "an older archive, changed after the backup",
                    Files.readString(older.toPath()));
        } finally {
            global = getGeoServer().getGlobal();
            global.setLockProviderName(lockProvider);
            getGeoServer().save(global);
        }
    }

    private void waitForCompletion(AbstractExecutionAdapter execution) throws Exception {
        int cnt = 0;
        while (cnt < 1200 && (execution.getStatus() != BatchStatus.COMPLETED || execution.isRunning())) {
            Thread.sleep(100);
            cnt++;
            if (execution.getStatus() == BatchStatus.ABANDONED
                    || execution.getStatus() == BatchStatus.FAILED
                    || execution.getStatus() == BatchStatus.UNKNOWN) {
                for (Throwable exception : execution.getAllFailureExceptions()) {
                    LOGGER.log(java.util.logging.Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                }
                break;
            }
        }
    }
}
