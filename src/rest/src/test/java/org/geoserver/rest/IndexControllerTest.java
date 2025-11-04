/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import jakarta.servlet.Filter;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class IndexControllerTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // only setup security, no data needed
        testData.setUpSecurity();
    }

    @Override
    protected List<Filter> getFilters() {
        return List.of(GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class));
    }

    @Test
    public void testRootWithoutExtensionAdministrator() throws Exception {
        doTestIndex("admin", "geoserver", "/rest");
    }

    @Test
    public void testRootWithExtensionAdministrator() throws Exception {
        doTestIndex("admin", "geoserver", "/rest.html");
    }

    @Test
    public void testIndexWithoutExtensionAdministrator() throws Exception {
        doTestIndex("admin", "geoserver", "/rest/index");
    }

    @Test
    public void testIndexWithExtensionAdministrator() throws Exception {
        doTestIndex("admin", "geoserver", "/rest/index.html");
    }

    @Test
    public void testRootWithoutExtensionAnonymous() throws Exception {
        doTestIndex(null, null, "/rest");
    }

    @Test
    public void testRootWithExtensionAnonymous() throws Exception {
        doTestIndex(null, null, "/rest.html");
    }

    @Test
    public void testIndexWithoutExtensionAnonymous() throws Exception {
        doTestIndex(null, null, "/rest/index");
    }

    @Test
    public void testIndexWithExtensionAnonymous() throws Exception {
        doTestIndex(null, null, "/rest/index.html");
    }

    private void doTestIndex(String username, String password, String path) throws Exception {
        setRequestAuth(username, password);
        MockHttpServletResponse response = getAsServletResponse(path);
        String content = response.getContentAsString();
        if (username != null) {
            assertEquals(200, response.getStatus());
            assertThat(content, containsString("Geoserver Configuration API"));
            assertThat(content, containsString("<a href="));
        } else {
            assertEquals(401, response.getStatus());
            assertEquals("", content);
        }
    }
}
