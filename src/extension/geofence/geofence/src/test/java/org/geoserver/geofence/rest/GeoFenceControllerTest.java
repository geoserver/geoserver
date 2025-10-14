/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest;

import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author Emanuele Tajariol (etj at geo-solutions.it) */
public class GeoFenceControllerTest extends GeoServerSystemTestSupport {

    protected static Catalog catalog;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        catalog = getCatalog();
    }

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testGetInfo() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/geofence/info");
        assertEquals(200, response.getStatus());
        assertContentType(MediaType.TEXT_PLAIN_VALUE, response);
        String content = response.getContentAsString();
        assertEquals("default-gs", content);
    }
}
