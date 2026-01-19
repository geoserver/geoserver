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
    public void testJsonpException() throws Exception {
        testJsonpException("1.1.0");
    }

    @Test
    public void testJsonException() throws Exception {
        testJsonException("1.1.0");
    }
}
