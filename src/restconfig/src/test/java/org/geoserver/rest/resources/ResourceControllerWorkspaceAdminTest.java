/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.Filter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.AccessMode;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.Before;
import org.junit.Test;
import org.kordamp.json.JSONObject;
import org.kordamp.json.test.JSONAssert;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Integration tests for {@link ResourceController} with workspace administrator access.
 *
 * <p>Verifies that the resource browser ({@code /rest/resource/**}) correctly enforces access control for workspace
 * administrators: full access to resources within their workspace, read-only access to collection listings, and no
 * access to resources in other workspaces.
 */
@TestSetup(run = TestSetupFrequency.ONCE)
public class ResourceControllerWorkspaceAdminTest extends GeoServerSystemTestSupport {

    private static final String ROLE_WS_ADMIN = "ROLE_WS_ADMIN";

    private static final String WS_ADMIN_USER = "wsresadmin";
    private static final String WS_ADMIN_PASSWORD = "wsresadmin";

    private static final String WORKSPACE = "restest";
    private static final String WORKSPACE_NS = "http://www.geoserver.org/restest";

    private static final String OTHER_WORKSPACE = "resother";
    private static final String OTHER_WORKSPACE_NS = "http://www.geoserver.org/resother";

    @Override
    protected List<Filter> getFilters() {
        List<Filter> filters = new ArrayList<>(super.getFilters());
        filters.add((Filter) GeoServerExtensions.bean("filterChainProxy"));
        return filters;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addWorkspace(WORKSPACE, WORKSPACE_NS, getCatalog());
        testData.addWorkspace(OTHER_WORKSPACE, OTHER_WORKSPACE_NS, getCatalog());

        addUser(WS_ADMIN_USER, WS_ADMIN_PASSWORD, null, List.of(ROLE_WS_ADMIN));
        addLayerAccessRule(WORKSPACE, "*", AccessMode.ADMIN, ROLE_WS_ADMIN);

        // create some test resources in the data directory
        writeResource("workspaces/%s/testfile.txt".formatted(WORKSPACE), "workspace content");
        writeResource("workspaces/%s/otherfile.txt".formatted(OTHER_WORKSPACE), "other content");
        writeResource("styles/teststyle.sld", "<sld/>");
    }

    private void writeResource(String path, String content) throws Exception {
        Resource res = getDataDirectory().get(path);
        try (OutputStreamWriter w = new OutputStreamWriter(res.out(), UTF_8)) {
            w.write(content);
        }
    }

    @Before
    public void loginAsWorkspaceAdmin() {
        setRequestAuth(WS_ADMIN_USER, WS_ADMIN_PASSWORD);
    }

    // -- Root and collection listings (read-only) --

    @Test
    public void testGetRootDirectory() throws Exception {
        // workspace admin should only see "workspaces" in the root listing,
        // not global config files (global.xml, wfs.xml, etc.) or other directories
        JSONObject json = (JSONObject) getAsJSON("/rest/resource?format=json");
        ((JSONObject) json.get("ResourceDirectory")).remove("lastModified");

        String expected =
                """
                {'ResourceDirectory': {
                  'name': '',
                  'children': {'child': [
                    {
                      'name': 'styles',
                      'link': {
                        'href': 'http://localhost:8080/geoserver/rest/resource/styles',
                        'rel': 'alternate',
                        'type': 'application/json'
                      }
                    },
                    {
                      'name': 'workspaces',
                      'link': {
                        'href': 'http://localhost:8080/geoserver/rest/resource/workspaces',
                        'rel': 'alternate',
                        'type': 'application/json'
                      }
                    }
                  ]}
                }}
                """;
        JSONAssert.assertEquals(expected, json);
    }

    @Test
    public void testGetWorkspacesCollection() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/resource/workspaces?format=json");
        JSONObject dir = json.getJSONObject("ResourceDirectory");
        dir.remove("lastModified");

        String content = json.toString();
        // should only see the administrable workspace, not the other one
        assertTrue("Should contain administrable workspace", content.contains(WORKSPACE));
        assertFalse("Should not contain non-administrable workspace", content.contains(OTHER_WORKSPACE));
    }

    @Test
    public void testRootIsReadOnly() throws Exception {
        // trying to create a file at root should fail
        MockHttpServletResponse response =
                putAsServletResponse("/rest/resource/rootfile.txt", "data", MediaType.TEXT_PLAIN_VALUE);
        // not writable for ws admin
        assertEquals(403, response.getStatus());
    }

    // -- Own workspace resources --

    @Test
    public void testReadOwnWorkspaceResource() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("/rest/resource/workspaces/%s/testfile.txt".formatted(WORKSPACE));
        assertEquals(200, response.getStatus());
        assertEquals("workspace content", response.getContentAsString());
    }

    @Test
    public void testReadOwnWorkspaceDirectory() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("/rest/resource/workspaces/%s?format=json".formatted(WORKSPACE));
        assertEquals(200, response.getStatus());

        String content = response.getContentAsString();
        assertTrue(content.contains("testfile.txt"));
    }

    @Test
    public void testUploadToOwnWorkspace() throws Exception {
        String content = "new file content";
        MockHttpServletResponse response = putAsServletResponse(
                "/rest/resource/workspaces/%s/uploaded.txt".formatted(WORKSPACE), content, MediaType.TEXT_PLAIN_VALUE);
        assertEquals(201, response.getStatus());

        // verify it was written
        Resource uploaded = getDataDirectory().get("workspaces/%s/uploaded.txt".formatted(WORKSPACE));
        assertTrue(Resources.exists(uploaded));

        // read it back through the API
        response = getAsServletResponse("/rest/resource/workspaces/%s/uploaded.txt".formatted(WORKSPACE));
        assertEquals(200, response.getStatus());
        assertEquals(content, response.getContentAsString());
    }

    @Test
    public void testDeleteOwnWorkspaceResource() throws Exception {
        writeResource("workspaces/%s/todelete.txt".formatted(WORKSPACE), "delete me");
        Resource res = getDataDirectory().get("workspaces/%s/todelete.txt".formatted(WORKSPACE));
        assertTrue(Resources.exists(res));

        MockHttpServletResponse response =
                deleteAsServletResponse("/rest/resource/workspaces/%s/todelete.txt".formatted(WORKSPACE));
        assertEquals(200, response.getStatus());

        assertFalse(Resources.exists(res));
    }

    @Test
    public void testMoveWithinOwnWorkspace() throws Exception {
        writeResource("workspaces/%s/moveme.txt".formatted(WORKSPACE), "move content");

        String path = "/rest/resource/workspaces/%s/moved.txt?operation=move".formatted(WORKSPACE);
        String body = "/workspaces/%s/moveme.txt".formatted(WORKSPACE);
        MockHttpServletResponse response = putAsServletResponse(path, body, "text/plain");
        assertEquals(201, response.getStatus());

        assertFalse(Resources.exists(getDataDirectory().get("workspaces/%s/moveme.txt".formatted(WORKSPACE))));
        assertTrue(Resources.exists(getDataDirectory().get("workspaces/%s/moved.txt".formatted(WORKSPACE))));
    }

    @Test
    public void testCopyWithinOwnWorkspace() throws Exception {
        writeResource("workspaces/%s/copyme.txt".formatted(WORKSPACE), "copy content");

        String path = "/rest/resource/workspaces/%s/copied.txt?operation=copy".formatted(WORKSPACE);
        String body = "/workspaces/%s/copyme.txt".formatted(WORKSPACE);
        MockHttpServletResponse response = putAsServletResponse(path, body, "text/plain");
        assertEquals(201, response.getStatus());

        // both should exist
        assertTrue(Resources.exists(getDataDirectory().get("workspaces/%s/copyme.txt".formatted(WORKSPACE))));
        assertTrue(Resources.exists(getDataDirectory().get("workspaces/%s/copied.txt".formatted(WORKSPACE))));
    }

    @Test
    public void testMetadataOwnWorkspaceResource() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "/rest/resource/workspaces/%s/testfile.txt?operation=metadata&format=json".formatted(WORKSPACE));
        assertEquals(200, response.getStatus());

        String content = response.getContentAsString();
        assertTrue(content.contains("testfile.txt"));
        assertTrue(content.contains("resource"));
    }

    // -- Other workspace resources (denied) --

    @Test
    public void testReadOtherWorkspaceResourceDenied() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("/rest/resource/workspaces/%s/otherfile.txt".formatted(OTHER_WORKSPACE));
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testListOtherWorkspaceDirectoryDenied() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("/rest/resource/workspaces/%s?format=json".formatted(OTHER_WORKSPACE));
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testUploadToOtherWorkspaceDenied() throws Exception {
        MockHttpServletResponse response = putAsServletResponse(
                "/rest/resource/workspaces/%s/hack.txt".formatted(OTHER_WORKSPACE),
                "malicious",
                MediaType.TEXT_PLAIN_VALUE);
        assertEquals(404, response.getStatus());

        // verify file was not created regardless of status code
        assertFalse(Resources.exists(getDataDirectory().get("workspaces/%s/hack.txt".formatted(OTHER_WORKSPACE))));
    }

    @Test
    public void testDeleteOtherWorkspaceResourceDenied() throws Exception {
        Resource res = getDataDirectory().get("workspaces/%s/otherfile.txt".formatted(OTHER_WORKSPACE));
        assertTrue(Resources.exists(res));

        MockHttpServletResponse response =
                deleteAsServletResponse("/rest/resource/workspaces/%s/otherfile.txt".formatted(OTHER_WORKSPACE));
        assertEquals(404, response.getStatus());

        // verify file still exists
        assertTrue(Resources.exists(res));
    }

    // -- Global styles resources (read-only) --

    @Test
    public void testGlobalStylesDirectoryListsAllStyles() throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/resource/styles?format=json");
        String content = json.toString();

        // should list all global styles, including our test style and built-in ones
        assertTrue(content.contains("teststyle.sld"));
        assertTrue(content.contains("default_point.sld"));
        assertTrue(content.contains("default_line.sld"));
        assertTrue(content.contains("default_polygon.sld"));
    }

    @Test
    public void testReadGlobalStyleResource() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("/rest/resource/styles/teststyle.sld");
        assertEquals(200, response.getStatus());
        assertEquals("<sld/>", response.getContentAsString());
    }

    @Test
    public void testUploadToGlobalStylesDenied() throws Exception {
        MockHttpServletResponse response =
                putAsServletResponse("/rest/resource/styles/injected.sld", "<sld/>", MediaType.TEXT_PLAIN_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDeleteGlobalStyleResourceDenied() throws Exception {
        MockHttpServletResponse response = deleteAsServletResponse("/rest/resource/styles/teststyle.sld");
        assertEquals(403, response.getStatus());

        // verify file still exists
        assertTrue(Resources.exists(getDataDirectory().get("styles/teststyle.sld")));
    }

    // -- Admin has full access --

    @Test
    public void testAdminCanAccessAllResources() throws Exception {
        setRequestAuth("admin", "geoserver");

        MockHttpServletResponse response =
                getAsServletResponse("/rest/resource/workspaces/%s/testfile.txt".formatted(WORKSPACE));
        assertEquals(200, response.getStatus());

        response = getAsServletResponse("/rest/resource/workspaces/%s/otherfile.txt".formatted(OTHER_WORKSPACE));
        assertEquals(200, response.getStatus());
    }

    // -- Resource headers --

    @Test
    public void testResourceHeadersOwnWorkspace() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("/rest/resource/workspaces/%s/testfile.txt".formatted(WORKSPACE));
        assertEquals(200, response.getStatus());

        assertNotNull(response.getHeader("Last-Modified"));
        assertEquals("resource", response.getHeader("Resource-Type"));
        assertNotNull(response.getHeader("Resource-Parent"));
    }

    @Test
    public void testDirectoryHeadersOwnWorkspace() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("/rest/resource/workspaces/%s?format=json".formatted(WORKSPACE));
        assertEquals(200, response.getStatus());

        assertEquals("directory", response.getHeader("Resource-Type"));
    }
}
