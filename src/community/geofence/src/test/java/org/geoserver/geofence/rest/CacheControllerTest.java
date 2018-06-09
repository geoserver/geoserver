/*
 *  Copyright (C) 2017 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geoserver.geofence.rest;

import static org.junit.Assert.*;

import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author Emanuele Tajariol (etj at geo-solutions.it) */
public class CacheControllerTest extends GeoServerSystemTestSupport {

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
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/geofence/ruleCache/info");
        assertEquals(200, response.getStatus());
        assertContentType(MediaType.TEXT_PLAIN_VALUE, response);
        String content = response.getContentAsString();
        assertTrue("Missing RuleStats", content.contains("RuleStats["));
        assertTrue("Missing AdminAuthStats", content.contains("AdminAuthStats["));
        assertTrue("Missing UserStats", content.contains("UserStats["));
    }

    @Test
    public void testGetInfoLegacy() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/ruleCache/info");
        assertEquals(200, response.getStatus());
        assertContentType(MediaType.TEXT_PLAIN_VALUE, response);
        String content = response.getContentAsString();
        assertTrue("Missing RuleStats", content.contains("RuleStats["));
        assertTrue("Missing AdminAuthStats", content.contains("AdminAuthStats["));
        assertTrue("Missing UserStats", content.contains("UserStats["));
    }

    @Test
    public void testInvalidate() throws Exception {
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/geofence/ruleCache/invalidate");
        assertEquals(200, response.getStatus());
        assertContentType(MediaType.TEXT_PLAIN_VALUE, response);
        String content = response.getContentAsString();
        assertEquals("OK", content);
    }
}
