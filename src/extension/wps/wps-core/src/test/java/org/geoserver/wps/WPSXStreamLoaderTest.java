package org.geoserver.wps;

import static org.easymock.classextension.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.geoserver.config.GeoServer;
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

    @Test
    public void testBackFormatXmlComatibility() throws Exception {
        GeoServer gs = createMock(GeoServer.class);
        URL url = Thread.currentThread().getContextClassLoader().getResource("org/geoserver/wps/");
        File file = new File(url.getPath());
        WPSXStreamLoader loader = new WPSXStreamLoader(new GeoServerResourceLoader(file));
        WPSInfo wps = loader.load(gs);
        boolean found1 = false;
        boolean found2 = false;
        for(ProcessGroupInfo pg : wps.getProcessGroups()){
            if(pg.getFactoryClass().getName().equals("org.geoserver.wps.DeprecatedProcessFactory")){
                assertFalse(pg.isEnabled());
                found1 = true;
            }
            if(pg.getFilteredProcesses() != null){
                for(Object opi : pg.getFilteredProcesses()){
                    assertTrue(opi instanceof ProcessInfo);
                }
                if(pg.getFactoryClass().getName().equals("org.geoserver.wps.jts.SpringBeanProcessFactory")){
                    assertTrue(pg.isEnabled());
                    assertEquals(pg.getFilteredProcesses().get(0).getName().toString(),"gs:GeorectifyCoverage");
                    assertEquals(pg.getFilteredProcesses().get(1).getName().toString(),"gs:GetFullCoverage");
                    assertEquals(pg.getFilteredProcesses().get(2).getName().toString(),"gs:Import");
                    found2 = true;
                }
            }
        }
        assertTrue(found1);
        assertTrue(found2);
    }
}
