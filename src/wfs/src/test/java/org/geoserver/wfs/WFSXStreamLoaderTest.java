package org.geoserver.wfs;

import static org.junit.Assert.*;

import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Test;

public class WFSXStreamLoaderTest {

    @Test
    public void testGmlCreateFromScratch() throws Exception {
        WFSXStreamLoader loader = new WFSXStreamLoader(new GeoServerResourceLoader());
        WFSInfo wfs = loader.createServiceFromScratch(null);
        assertNotNull(wfs);

        assertTrue(wfs.getGML().containsKey(WFSInfo.Version.V_10));
        assertTrue(wfs.getGML().containsKey(WFSInfo.Version.V_11));
        assertTrue(wfs.getGML().containsKey(WFSInfo.Version.V_20));
    }
}
