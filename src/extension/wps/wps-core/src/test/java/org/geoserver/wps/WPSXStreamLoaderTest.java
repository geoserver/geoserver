package org.geoserver.wps;

import static org.junit.Assert.*;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Test;

public class WPSXStreamLoaderTest extends WPSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed for this test
    }

    @Test
    public void testCreateFromScratch() throws Exception {
        WPSXStreamLoader loader = GeoServerExtensions.bean(WPSXStreamLoader.class);
        WPSInfo wps = loader.createServiceFromScratch(null);
        assertNotNull(wps);
        assertEquals("WPS", wps.getName());
    }
    
    @Test
    public void testInit() throws Exception {
        WPSXStreamLoader loader = GeoServerExtensions.bean(WPSXStreamLoader.class);
        WPSInfo wps = new WPSInfoImpl();
        loader.initializeService(wps);
        assertEquals("WPS", wps.getName());
    }
}
