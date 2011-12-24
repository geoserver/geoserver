package org.geoserver.wps.resource;

import java.io.File;

import org.geoserver.wps.WPSTestSupport;

public class WPSResourceManagerTest extends WPSTestSupport {

    WPSResourceManager resourceMgr;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        resourceMgr = new WPSResourceManager();
    }

    public void testAddResourceNoExecutionId() throws Exception {
        File f = File.createTempFile("dummy", "dummy", new File("target"));
        resourceMgr.addResource(new WPSFileResource(f));
    }
}