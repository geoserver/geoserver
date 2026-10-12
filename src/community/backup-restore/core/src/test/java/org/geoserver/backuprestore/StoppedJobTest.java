/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.backuprestore.tasklet.AbstractCatalogBackupRestoreTasklet;
import org.geoserver.platform.resource.Files;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

/**
 * The tasklets are singletons shared by every job, so a stop requested for one job must not reach the next. The stop
 * flag used to stay set, and any later step that outlasted one check interval ended STOPPED.
 */
public class StoppedJobTest extends BackupRestoreTestSupport {

    @Override
    @Before
    public void beforeTest() throws InterruptedException {
        ensureCleanedQueues();
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testAStopDoesNotReachTheNextJob() throws Exception {
        AbstractCatalogBackupRestoreTasklet<?> tasklet =
                applicationContext.getBean("catalogBackupTasklet", AbstractCatalogBackupRestoreTasklet.class);
        // check at once, so that the step is still running at the first check
        tasklet.setTerminationCheckInterval(1);
        // what JobOperator.stop() does to the running tasklet of an earlier job
        tasklet.stop();
        try {
            File archive = File.createTempFile("stoppedJobTest", ".zip");
            archive.deleteOnExit();
            Map<String, String> params = new HashMap<>();
            params.put(Backup.PARAM_BEST_EFFORT_MODE, "true");
            BackupExecutionAdapter backup =
                    backupFacade.runBackupAsync(Files.asResource(archive), true, null, null, null, params);

            int cnt = 0;
            while (cnt < 600 && (backup.getStatus() != BatchStatus.COMPLETED || backup.isRunning())) {
                Thread.sleep(100);
                cnt++;
                if (backup.getStatus() == BatchStatus.FAILED
                        || backup.getStatus() == BatchStatus.STOPPED
                        || backup.getStatus() == BatchStatus.ABANDONED) {
                    break;
                }
            }
            assertEquals(BatchStatus.COMPLETED, backup.getStatus());
        } finally {
            tasklet.setTerminationCheckInterval(1000);
        }
    }
}
