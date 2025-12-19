package org.geoserver.wfs;

import org.geoserver.data.test.SystemTestData;
import org.junit.Test;

public class WFSServiceExceptionTest extends WFSServiceExceptionTestSupport {
    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(true);
        getGeoServer().save(wfs);
    }

    @Test
    public void testJsonpException20() throws Exception {
        testJsonpException("2.0.0");
    }

    @Test
    public void testJsonException20() throws Exception {
        testJsonException("2.0.0");
    }
}
