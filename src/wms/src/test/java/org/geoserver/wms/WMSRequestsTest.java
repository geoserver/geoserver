/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertTrue;

import java.net.URLDecoder;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpMap;
import org.junit.Test;

public class WMSRequestsTest extends WMSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addDefaultRasterLayer(MockData.TASMANIA_DEM, getCatalog());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetGetMapUrlWithDimensions() throws Exception {
        GetMapRequest request = createGetMapRequest(MockData.TASMANIA_DEM);
        KvpMap rawKvp = new KvpMap(request.getRawKvp());
        rawKvp.put("time", "2017-04-07T19:56:00.000Z");
        rawKvp.put("elevation", "1013.2");
        rawKvp.put("dim_my_dimension", "010");
        request.setRawKvp(rawKvp);
        request.setFormat(DefaultWebMapService.FORMAT);
        DefaultWebMapService.autoSetBoundsAndSize(request);
        String url =
                WMSRequests.getGetMapUrl(
                        request, request.getLayers().get(0).getName(), 0, null, null, null);
        url = URLDecoder.decode(url, "UTF-8");
        assertTrue(
                "Missing time in GetMap URL: " + url,
                url.contains("&time=2017-04-07T19:56:00.000Z"));
        assertTrue("Missing elevation in GetMap URL: " + url, url.contains("&elevation=1013.2"));
        assertTrue(
                "Missing custom dimension in GetMap URL: " + url,
                url.contains("&dim_my_dimension=010"));
    }
}
