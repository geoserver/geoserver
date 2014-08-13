package org.geoserver.wps;

import static org.junit.Assert.*;

import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Test;

public class WPSXStreamLoaderTest {

    @Test
    public void testCreateFromScratch() throws Exception {
        WPSXStreamLoader loader = new WPSXStreamLoader(new GeoServerResourceLoader());
        WPSInfo wps = loader.createServiceFromScratch(null);
        assertNotNull(wps);
        assertEquals("WPS", wps.getName());
    }
    
    @Test
    public void testInit() throws Exception {
        WPSXStreamLoader loader = new WPSXStreamLoader(new GeoServerResourceLoader());
        WPSInfo wps = new WPSInfoImpl();
        loader.initializeService(wps);
        assertEquals("WPS", wps.getName());
    }
}
