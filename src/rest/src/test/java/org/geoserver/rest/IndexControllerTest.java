/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import jakarta.servlet.Filter;
import java.util.List;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class IndexControllerTest extends GeoServerSystemTestSupport {

    private final String wsadminUser = "wsadminUser";
    private final String wsadminPwd = "wsadminPwd";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // only setup security, no data needed
        testData.setUpSecurity();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        WorkspaceInfo workspace = getCatalog().getWorkspaces().get(0);
        addUser(wsadminUser, wsadminPwd, null, List.of("ROLE_WSADMIN"));
        // add workspace admin rule
        addLayerAccessRule(workspace.getName(), "*", AccessMode.ADMIN, "ROLE_WSADMIN");
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

    @Test
    public void testRootWithoutExtensionWorkspaceAdmin() throws Exception {
        doTestIndex(wsadminUser, wsadminPwd, "/rest");
    }

    @Test
    public void testRootWithExtensionWorkspaceAdmin() throws Exception {
        doTestIndex(wsadminUser, wsadminPwd, "/rest.html");
    }

    @Test
    public void testIndexWithoutExtensionWorkspaceAdmin() throws Exception {
        doTestIndex(wsadminUser, wsadminPwd, "/rest/index");
    }

    @Test
    public void testIndexWithExtensionWorkspaceAdmin() throws Exception {
        doTestIndex(wsadminUser, wsadminPwd, "/rest/index.html");
    }

    @Test
    public void testIndexLimitedContentsForWorkspaceAdmin() throws Exception {
        String adminContents = doTestIndex("admin", "geoserver", "/rest");
        assertThat(adminContents, containsString("href=\"http://localhost:8080/geoserver/rest/index\""));
        assertThat(adminContents, containsString("href=\"http://localhost:8080/geoserver/rest/gsuser\""));

        String workspaceAdminContent = doTestIndex(wsadminUser, wsadminPwd, "/rest");
        assertThat(workspaceAdminContent, containsString("href=\"http://localhost:8080/geoserver/rest/index\""));
        assertThat(workspaceAdminContent, not(containsString("href=\"http://localhost:8080/geoserver/rest/gsuser\"")));
    }

    private String doTestIndex(String username, String password, String path) throws Exception {
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
        return content;
    }
}
