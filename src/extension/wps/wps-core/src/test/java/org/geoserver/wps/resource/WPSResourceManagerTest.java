/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import org.apache.commons.io.FileUtils;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WPSResourceManagerTest extends WPSTestSupport {

    WPSResourceManager resourceMgr;

    ProcessStatusTracker tracker;

    private static final File WPS_RESOURCE_DIR = new File("target/gs_datadir/tmp/wps");

    @Before
    public void setUpInternal() throws Exception {
        resourceMgr = new WPSResourceManager();
        resourceMgr.setApplicationContext(applicationContext);

        if (WPS_RESOURCE_DIR.exists()) {
            FileUtils.deleteDirectory(WPS_RESOURCE_DIR);
        }
        WPS_RESOURCE_DIR.mkdirs();
        FileSystemResourceStore resourceStore = new FileSystemResourceStore(WPS_RESOURCE_DIR);
        DefaultProcessArtifactsStore artifactsStore =
                (DefaultProcessArtifactsStore) resourceMgr.getArtifactsStore();
        artifactsStore.setResourceStore(resourceStore);

        tracker = new ProcessStatusTracker();
        tracker.setApplicationContext(null);
    }

    @After
    public void cleanUp() throws Exception {
        if (WPS_RESOURCE_DIR.exists()) {
            FileUtils.deleteDirectory(WPS_RESOURCE_DIR);
        }
    }

    @Test
    public void testAddResourceNoExecutionId() throws Exception {
        File f = File.createTempFile("dummy", "dummy", new File("target"));
        resourceMgr.addResource(new WPSFileResource(f));
    }

    @Test
    public void testCleanupResource() throws Exception {
        String executionId = resourceMgr.getExecutionId(true);

        // Output resource
        Resource result = resourceMgr.getOutputResource(executionId, "test.txt");
        File file = result.file();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Write some dummy content
            fos.write("dummy".getBytes());
        }

        // Temporary resource
        Resource temp = resourceMgr.getTemporaryResource("tmp");
        File tempFile = temp.file();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            // Write some content
            fos.write("temptemptemp".getBytes());
        }

        assertTrue(file.exists());
        assertTrue(tempFile.exists());

        // Check the processId folder exists
        File processDir = new File(WPS_RESOURCE_DIR, executionId);
        assertTrue(processDir.exists());
        assertTrue(processDir.isDirectory());
        assertEquals(2, processDir.listFiles().length); // 2 subfolders exists (out and tmp)

        // Sleep a few milliseconds
        Thread.sleep(2);
        resourceMgr.cleanExpiredResources(System.currentTimeMillis(), tracker);

        // Check the processId folder doesn't exist anymore
        assertFalse(processDir.exists());
    }
}
