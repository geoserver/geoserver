package org.geoserver.wps;

import static org.junit.Assert.assertNotNull;

import org.geoserver.data.test.SystemTestData;
import org.junit.Test;

public class GeoServerFactoryExtensionTest extends WPSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do nothing, we don't need data for this test
    }
    
    @Test
    public void testWPSFactoryExtension() {
        WPSInfo info = getGeoServer().getFactory().create(WPSInfo.class);
        assertNotNull(info);
    }
    
}
