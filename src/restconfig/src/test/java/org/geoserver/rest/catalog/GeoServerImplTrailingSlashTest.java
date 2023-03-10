/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.assertEquals;

import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Test for GeoServerImpl trailing slash match */
public class GeoServerImplTrailingSlashTest extends GeoServerSystemTestSupport {
    private static String BASEPATH = RestBaseController.ROOT_PATH;

    @After
    public void clearEnviromentVariables() {
        System.clearProperty(GeoServerInfoImpl.TRAILING_SLASH_MATCH_KEY);
    }

    @Override
    protected SystemTestData createTestData() throws Exception {
        return new GeoServerImplTrailingSlashTestData();
    }

    @Test
    public void testSetTrailinSlashMatchFalse() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(BASEPATH + "/about/status/", null);
        assertEquals(404, response.getStatus());
        MockHttpServletResponse responseNoSlash =
                getAsServletResponse(BASEPATH + "/about/status", null);
        assertEquals(200, responseNoSlash.getStatus());
    }
}
