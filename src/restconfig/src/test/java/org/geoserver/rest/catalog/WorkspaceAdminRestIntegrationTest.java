/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import javax.servlet.Filter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.AccessMode;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Integration tests for the workspace administrator REST API functionality.
 *
 * <p>These tests verify that workspace administrators have appropriate access to REST API endpoints according to the
 * security rules defined in rest.workspaceadmin.properties.
 *
 * <p>This test suite comprehensively covers:
 *
 * <ul>
 *   <li>Admin-only endpoints - Verifies workspace admins cannot access administrative endpoints
 *   <li>Workspace and namespace operations - Read access to all, modify access only to assigned workspaces
 *   <li>Style operations - Read access to global styles, full access to workspace-specific styles
 *   <li>Layer operations - Access limited to assigned workspaces
 *   <li>LayerGroup operations - No access to global layer groups, full access to workspace-specific layer groups
 *   <li>Template operations - Read access to global templates, full access to workspace-specific templates
 *   <li>Settings operations - Full access to settings for assigned workspaces
 *   <li>Service settings - Full access to service configuration for assigned workspaces
 *   <li>ResourceStore operations - Access limited to assigned workspaces
 * </ul>
 *
 * <p>All tests are run as a workspace admin user with WORKSPACE_ADMIN role and specific access rights to a test
 * workspace, ensuring proper permission boundaries are enforced by the REST API.
 */
public class WorkspaceAdminRestIntegrationTest extends GeoServerSystemTestSupport {

    private static final String WORKSPACE_ADMIN_USER = "wsadmin";
    private static final String WORKSPACE_ADMIN_PASSWORD = "wsadmin";
    private static final String WORKSPACE_ADMIN_WORKSPACE = "wsadmintest";
    private static final String WORKSPACE_ADMIN_NAMESPACE = "http://www.geoserver.org/wsadmintest";
    private static final String WORKSPACE_NO_ACCESS = "noaccess";
    private static final String WORKSPACE_NO_ACCESS_NAMESPACE = "http://www.geoserver.org/noaccess";

    private static final String TEST_GLOBAL_STYLE = "test_global_style";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // Create test workspaces
        addWorkspace(WORKSPACE_ADMIN_WORKSPACE, WORKSPACE_ADMIN_NAMESPACE);
        addWorkspace(WORKSPACE_NO_ACCESS, WORKSPACE_NO_ACCESS_NAMESPACE);

        // Create a test global style for deletion tests
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(TEST_GLOBAL_STYLE);
        style.setFilename(TEST_GLOBAL_STYLE + ".sld");
        catalog.add(style);

        // Create the workspace admin user and role
        GeoServerSecurityManager securityManager = getSecurityManager();
        GeoServerSecurityTestSupport.createUserWithRole(
                securityManager,
                WORKSPACE_ADMIN_USER,
                WORKSPACE_ADMIN_PASSWORD,
                GeoServerRole.WORKSPACE_ADMIN_ROLE.getAuthority());

        // Add access rules for the workspace admin
        DataAccessRuleDAO dao = DataAccessRuleDAO.get();
        DataAccessRule rule = new DataAccessRule();
        rule.setWorkspace(WORKSPACE_ADMIN_WORKSPACE);
        rule.setLayer("*");
        rule.setAccessMode(AccessMode.ADMIN);
        rule.getRoles().add(GeoServerRole.WORKSPACE_ADMIN_ROLE.getAuthority());
        dao.addRule(rule);
        dao.storeRules();
    }

    @Override
    protected List<Filter> getFilters() {
        // Enable Spring security
        return Collections.singletonList((Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    private void addWorkspace(String name, String uri) {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getFactory().createWorkspace();
        ws.setName(name);
        NamespaceInfo ns = catalog.getFactory().createNamespace();
        ns.setPrefix(name);
        ns.setURI(uri);

        catalog.add(ws);
        catalog.add(ns);
    }

    private void setupTestLayer(String workspaceName) throws Exception {
        // Create a test datastore and layer in the specified workspace
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getWorkspaceByName(workspaceName);

        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace(ws);

        DataStoreInfo ds = builder.buildDataStore(workspaceName + "Store");
        ds.setType("H2");
        ds.getConnectionParameters().put("dbtype", "h2");
        ds.getConnectionParameters().put("database", getTestData().getDataDirectoryRoot() + "/" + workspaceName);
        catalog.add(ds);

        // Create a test style
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(workspaceName + "Style");
        style.setWorkspace(ws);
        style.setFilename(workspaceName + "Style.sld");
        catalog.add(style);
    }

    //
    // Workspace access tests
    //

    @Test
    public void testListWorkspaces() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request the workspaces list (should be accessible read-only)
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(getBinaryInputStream(response));
        // Should see all workspaces in the list
        assertXpathExists("//workspace[name='" + WORKSPACE_ADMIN_WORKSPACE + "']", dom);
        assertXpathExists("//workspace[name='" + WORKSPACE_NO_ACCESS + "']", dom);
        assertXpathExists("//workspace[name='cite']", dom);
    }

    @Test
    public void testGetWorkspaceWithAccess() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request the workspace the admin has access to
        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + ".xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo(WORKSPACE_ADMIN_WORKSPACE, "/workspace/name", dom);
    }

    @Test
    public void testGetWorkspaceWithoutAccess() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Workspace should be visible in the list (read-only)
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_NO_ACCESS + ".xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo(WORKSPACE_NO_ACCESS, "/workspace/name", dom);
    }

    @Test
    public void testUpdateWorkspaceWithAccess() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Update a workspace (should be able to update but not rename)
        String updateXml = "<workspace><name>" + WORKSPACE_ADMIN_WORKSPACE + "</name></workspace>";
        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE,
                updateXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRenameWorkspaceDenied() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Try to rename a workspace (should be denied)
        String renameXml = "<workspace><name>renamed</name></workspace>";
        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE,
                renameXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testUpdateWorkspaceWithoutAccess() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Try to update a workspace the admin doesn't have access to
        String updateXml = "<workspace><name>" + WORKSPACE_NO_ACCESS + "</name></workspace>";
        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_NO_ACCESS,
                updateXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDeleteWorkspaceDenied() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Try to delete a workspace (should be denied)
        MockHttpServletResponse response =
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE);
        assertEquals(403, response.getStatus());
    }

    //
    // Namespace access tests
    //

    @Test
    public void testListNamespaces() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request the namespaces list (should be accessible read-only)
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/namespaces.xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(getBinaryInputStream(response));
        // Should see all namespaces in the list
        assertXpathExists("//namespace[prefix='" + WORKSPACE_ADMIN_WORKSPACE + "']", dom);
        assertXpathExists("//namespace[prefix='" + WORKSPACE_NO_ACCESS + "']", dom);
        assertXpathExists("//namespace[prefix='cite']", dom);
    }

    @Test
    public void testGetNamespaceWithAccess() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request the namespace the admin has access to
        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/namespaces/" + WORKSPACE_ADMIN_WORKSPACE + ".xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo(WORKSPACE_ADMIN_WORKSPACE, "/namespace/prefix", dom);
        assertXpathEvaluatesTo(WORKSPACE_ADMIN_NAMESPACE, "/namespace/uri", dom);
    }

    @Test
    public void testUpdateNamespaceWithAccess() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Update a namespace URI (should be allowed)
        String updateXml = "<namespace><prefix>" + WORKSPACE_ADMIN_WORKSPACE
                + "</prefix><uri>http://updated-uri</uri></namespace>";
        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/namespaces/" + WORKSPACE_ADMIN_WORKSPACE,
                updateXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());

        // Verify the URI was updated
        response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/namespaces/" + WORKSPACE_ADMIN_WORKSPACE + ".xml");
        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("http://updated-uri", "/namespace/uri", dom);
    }

    @Test
    public void testRenameNamespaceDenied() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Try to rename a namespace (should be denied)
        String renameXml =
                "<namespace><prefix>renamed</prefix><uri>" + WORKSPACE_ADMIN_NAMESPACE + "</uri></namespace>";
        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/namespaces/" + WORKSPACE_ADMIN_WORKSPACE,
                renameXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    //
    // Styles access tests
    //

    @Test
    public void testListGlobalStyles() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request the global styles list (should be accessible read-only)
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/styles.xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(getBinaryInputStream(response));
        // Should see global styles
        assertXpathExists("//style[name='generic']", dom);
        assertXpathExists("//style[name='line']", dom);
        assertXpathExists("//style[name='" + TEST_GLOBAL_STYLE + "']", dom);
    }

    @Test
    public void testGetGlobalStyle() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request a global style (should be accessible read-only)
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/styles/" + TEST_GLOBAL_STYLE + ".xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo(TEST_GLOBAL_STYLE, "/style/name", dom);
    }

    @Test
    public void testUpdateGlobalStyleDenied() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Try to update a global style (should be denied)
        String updateXml = "<style><name>" + TEST_GLOBAL_STYLE + "</name><filename>" + TEST_GLOBAL_STYLE
                + ".sld</filename></style>";
        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/styles/" + TEST_GLOBAL_STYLE,
                updateXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testCreateGlobalStyleDenied() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Try to create a new global style (should be denied)
        String createXml = "<style><name>newGlobalStyle</name><filename>newGlobalStyle.sld</filename></style>";
        MockHttpServletResponse response = postAsServletResponse(
                RestBaseController.ROOT_PATH + "/styles", createXml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDeleteGlobalStyleDenied() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Try to delete the test global style (should be denied)
        MockHttpServletResponse response =
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/styles/" + TEST_GLOBAL_STYLE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testWorkspaceSpecificStyles() throws Exception {
        // Create a test style in the workspace
        setupTestLayer(WORKSPACE_ADMIN_WORKSPACE);

        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request workspace styles
        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/styles.xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(getBinaryInputStream(response));
        assertXpathExists("//style[name='" + WORKSPACE_ADMIN_WORKSPACE + "Style']", dom);

        // Update a workspace style (should be allowed)
        String updateXml = "<style><name>" + WORKSPACE_ADMIN_WORKSPACE + "Style</name><filename>"
                + WORKSPACE_ADMIN_WORKSPACE + "Style.sld</filename></style>";
        response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/styles/"
                        + WORKSPACE_ADMIN_WORKSPACE + "Style",
                updateXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());
    }

    //
    // Layer group access tests
    //

    @Test
    public void testGlobalLayerGroupsDenied() throws Exception {
        // Create a global layer group for testing
        login("admin", "geoserver");

        // Create a global layer group if it doesn't exist already
        Catalog catalog = getCatalog();
        if (catalog.getLayerGroupByName("testGlobalLayerGroup") == null) {
            LayerGroupInfo layerGroup = catalog.getFactory().createLayerGroup();
            layerGroup.setName("testGlobalLayerGroup");
            catalog.add(layerGroup);
        }

        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Test all operations on global layer groups (all should be denied)

        // Try to list global layer groups (should be denied)
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups.xml");
        assertEquals(403, response.getStatus());

        // Try to get a global layer group (should be denied)
        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups/testGlobalLayerGroup.xml");
        assertEquals(403, response.getStatus());

        // Try to update a global layer group (should be denied)
        String updateXml = "<layerGroup><name>testGlobalLayerGroup</name><title>Updated Title</title></layerGroup>";
        response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/layergroups/testGlobalLayerGroup",
                updateXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());

        // Try to create a new global layer group (should be denied)
        String createXml = "<layerGroup><name>newGlobalLayerGroup</name><title>New Title</title></layerGroup>";
        response = postAsServletResponse(
                RestBaseController.ROOT_PATH + "/layergroups", createXml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());

        // Try to delete a global layer group (should be denied)
        response = deleteAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups/testGlobalLayerGroup");
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testWorkspaceSpecificLayerGroups() throws Exception {
        // Setup test layer
        setupTestLayer(WORKSPACE_ADMIN_WORKSPACE);

        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Create a workspace-specific layer group
        String createXml =
                "<layerGroup><name>workspaceLayerGroup</name><title>Workspace Layer Group</title></layerGroup>";
        MockHttpServletResponse response = postAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/layergroups",
                createXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(201, response.getStatus());

        // Verify the layer group exists
        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                + "/layergroups/workspaceLayerGroup.xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("workspaceLayerGroup", "/layerGroup/name", dom);

        // Update the workspace layer group
        String updateXml =
                "<layerGroup><name>workspaceLayerGroup</name><title>Updated Workspace Layer Group</title></layerGroup>";
        response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                        + "/layergroups/workspaceLayerGroup",
                updateXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());

        // Verify the update
        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                + "/layergroups/workspaceLayerGroup.xml");
        dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("Updated Workspace Layer Group", "/layerGroup/title", dom);

        // Try to create a layer group in an unauthorized workspace (should be denied)
        response = postAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_NO_ACCESS + "/layergroups",
                createXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());

        // Delete the workspace layer group
        response = deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                + "/layergroups/workspaceLayerGroup");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testWorkspaceLayerGroupWithGlobalStyle() throws Exception {
        // Setup test layer
        setupTestLayer(WORKSPACE_ADMIN_WORKSPACE);

        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Create a workspace-specific layer group that references a global style
        Catalog catalog = getCatalog();
        StyleInfo globalStyle = catalog.getStyleByName("line");
        assertNotNull("Global style 'line' not found", globalStyle);

        // Get a layer from the workspace
        String layerName = WORKSPACE_ADMIN_WORKSPACE + ":Store";

        // Create layer group with reference to global style
        String createXml = "<layerGroup>" + "  <name>globalStyleLayerGroup</name>"
                + "  <title>Layer Group With Global Style</title>"
                + "  <publishables>"
                + "    <published type=\"layer\">"
                + "      <name>"
                + layerName + "</name>" + "    </published>"
                + "  </publishables>"
                + "  <styles>"
                + "    <style>"
                + "      <name>line</name>"
                + "    </style>"
                + "  </styles>"
                + "</layerGroup>";

        MockHttpServletResponse response = postAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/layergroups",
                createXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(201, response.getStatus());

        // Verify the layer group exists and references the global style
        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                + "/layergroups/globalStyleLayerGroup.xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("globalStyleLayerGroup", "/layerGroup/name", dom);
        assertXpathEvaluatesTo("line", "//style/name", dom);

        // Delete the layer group
        response = deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                + "/layergroups/globalStyleLayerGroup");
        assertEquals(200, response.getStatus());
    }

    //
    // Templates access tests
    //

    @Test
    public void testListGlobalTemplates() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request the global templates list (should be accessible read-only)
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/templates.json");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testGetGlobalTemplate() throws Exception {
        // Create a test global template for testing
        login("admin", "geoserver");
        String templateContent = "This is a test template for ${name}";
        MockHttpServletResponse adminResponse = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/templates/test_template.ftl",
                templateContent,
                MediaType.TEXT_PLAIN_VALUE);
        assertEquals(200, adminResponse.getStatus());

        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request a global template (should be accessible read-only)
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/templates/test_template.ftl");
        assertEquals(200, response.getStatus());
        assertEquals(templateContent, response.getContentAsString());
    }

    @Test
    public void testUpdateGlobalTemplateDenied() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Try to update a global template (should be denied)
        String templateContent = "This is an updated template";
        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/templates/test_template.ftl",
                templateContent,
                MediaType.TEXT_PLAIN_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testWorkspaceSpecificTemplates() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Test creating a workspace-specific template
        String templateContent = "This is a workspace-specific template for ${name}";
        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                        + "/templates/workspace_template.ftl",
                templateContent,
                MediaType.TEXT_PLAIN_VALUE);
        // Template endpoints return 200 for both create and update
        assertEquals(200, response.getStatus());

        // Verify the template exists
        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                + "/templates/workspace_template.ftl");
        assertEquals(200, response.getStatus());
        assertEquals(templateContent, response.getContentAsString());

        // Try to create a template in an unauthorized workspace (should be denied)
        response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_NO_ACCESS
                        + "/templates/workspace_template.ftl",
                templateContent,
                MediaType.TEXT_PLAIN_VALUE);
        assertEquals(403, response.getStatus());
    }

    //
    // Layer access tests
    //

    @Test
    public void testListLayers() throws Exception {
        // Create test layers
        setupTestLayer(WORKSPACE_ADMIN_WORKSPACE);
        setupTestLayer(WORKSPACE_NO_ACCESS);

        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request all layers
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/layers.xml");
        assertEquals(200, response.getStatus());

        String content = response.getContentAsString();
        // Should only see layers from workspaces the admin has access to
        assertThat(content, containsString("<name>" + WORKSPACE_ADMIN_WORKSPACE + ":"));
        assertThat(content, not(containsString("<name>" + WORKSPACE_NO_ACCESS + ":")));
    }

    @Test
    public void testCreateLayerInWorkspace() throws Exception {
        // Create a datastore in the workspace
        setupTestLayer(WORKSPACE_ADMIN_WORKSPACE);

        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Create a new layer in the workspace
        String layerXml = "<featureType><name>testLayer</name><nativeName>testLayer</nativeName></featureType>";
        MockHttpServletResponse response = postAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/datastores/"
                        + WORKSPACE_ADMIN_WORKSPACE + "Store/featuretypes",
                layerXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(201, response.getStatus());

        // Verify the layer exists
        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                + "/datastores/" + WORKSPACE_ADMIN_WORKSPACE + "Store/featuretypes/testLayer.xml");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testCreateLayerInUnauthorizedWorkspace() throws Exception {
        // Create a datastore in both workspaces
        setupTestLayer(WORKSPACE_ADMIN_WORKSPACE);
        setupTestLayer(WORKSPACE_NO_ACCESS);

        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Try to create a new layer in the unauthorized workspace
        String layerXml = "<featureType><name>testLayer</name><nativeName>testLayer</nativeName></featureType>";
        MockHttpServletResponse response = postAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_NO_ACCESS + "/datastores/"
                        + WORKSPACE_NO_ACCESS + "Store/featuretypes",
                layerXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    //
    // REST Resource access tests
    //

    @Test
    public void testListResources() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request resource listing
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/resource.json");
        assertEquals(200, response.getStatus());

        // Request workspace resources
        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/resource/workspaces.json");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testAccessWorkspaceResources() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Access workspace resources
        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/resource/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + ".json");
        assertEquals(200, response.getStatus());

        // Try to access unauthorized workspace resources
        response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/resource/workspaces/" + WORKSPACE_NO_ACCESS + ".json");
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testCreateWorkspaceResource() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Create a resource in the workspace
        String content = "Test resource content";
        MockHttpServletResponse response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/resource/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/test.txt",
                content,
                MediaType.TEXT_PLAIN_VALUE);
        assertEquals(201, response.getStatus());

        // Verify the resource exists
        response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/resource/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/test.txt");
        assertEquals(200, response.getStatus());
        assertEquals(content, response.getContentAsString());

        // Try to create a resource in an unauthorized workspace
        response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/resource/workspaces/" + WORKSPACE_NO_ACCESS + "/test.txt",
                content,
                MediaType.TEXT_PLAIN_VALUE);
        assertEquals(403, response.getStatus());
    }

    //
    // Workspace-specific settings tests
    //

    @Test
    public void testWorkspaceSettings() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Test accessing workspace settings
        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/settings.xml");
        assertEquals(200, response.getStatus());

        // Test updating workspace settings
        String settingsXml = "<settings>" + "  <contact><addressCity>TestCity</addressCity></contact>"
                + "  <charset>UTF-8</charset>"
                + "  <numDecimals>8</numDecimals>"
                + "  <verbose>false</verbose>"
                + "  <verboseExceptions>false</verboseExceptions>"
                + "</settings>";

        response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/settings",
                settingsXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());

        // Verify settings were updated
        response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/settings.xml");
        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("TestCity", "//contact/addressCity", dom);
        assertXpathEvaluatesTo("8", "//numDecimals", dom);

        // Test accessing unauthorized workspace settings
        response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/workspaces/" + WORKSPACE_NO_ACCESS + "/settings.xml");
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testWorkspaceServiceSettings() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Test WMS service settings

        // First check if we can access the WMS settings
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH
                + "/services/wms/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/settings.xml");

        // If the settings don't exist yet, create them
        if (response.getStatus() == 404) {
            String wmsSettings = "<wms>" + "  <enabled>true</enabled>"
                    + "  <name>WMS</name>"
                    + "  <title>GeoServer Web Map Service</title>"
                    + "  <maintainer>Test Maintainer</maintainer>"
                    + "</wms>";

            response = postAsServletResponse(
                    RestBaseController.ROOT_PATH + "/services/wms/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                            + "/settings",
                    wmsSettings,
                    MediaType.APPLICATION_XML_VALUE);
            assertEquals(201, response.getStatus());
        }

        // Update WMS settings
        String updatedWmsSettings = "<wms>" + "  <enabled>true</enabled>"
                + "  <name>WMS</name>"
                + "  <title>Updated WMS Title</title>"
                + "  <maintainer>Updated Maintainer</maintainer>"
                + "</wms>";

        response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/services/wms/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/settings",
                updatedWmsSettings,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());

        // Verify WMS settings were updated
        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/services/wms/workspaces/"
                + WORKSPACE_ADMIN_WORKSPACE + "/settings.xml");
        assertEquals(200, response.getStatus());
        Document dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("Updated WMS Title", "//title", dom);
        assertXpathEvaluatesTo("Updated Maintainer", "//maintainer", dom);

        // Test WFS service settings

        // First check if we can access the WFS settings
        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/services/wfs/workspaces/"
                + WORKSPACE_ADMIN_WORKSPACE + "/settings.xml");

        // If the settings don't exist yet, create them
        if (response.getStatus() == 404) {
            String wfsSettings = "<wfs>" + "  <enabled>true</enabled>"
                    + "  <name>WFS</name>"
                    + "  <title>GeoServer Web Feature Service</title>"
                    + "  <maintainer>Test Maintainer</maintainer>"
                    + "</wfs>";

            response = postAsServletResponse(
                    RestBaseController.ROOT_PATH + "/services/wfs/workspaces/" + WORKSPACE_ADMIN_WORKSPACE
                            + "/settings",
                    wfsSettings,
                    MediaType.APPLICATION_XML_VALUE);
            assertEquals(201, response.getStatus());
        }

        // Update WFS settings
        String updatedWfsSettings = "<wfs>" + "  <enabled>true</enabled>"
                + "  <name>WFS</name>"
                + "  <title>Updated WFS Title</title>"
                + "  <maintainer>Updated Maintainer</maintainer>"
                + "</wfs>";

        response = putAsServletResponse(
                RestBaseController.ROOT_PATH + "/services/wfs/workspaces/" + WORKSPACE_ADMIN_WORKSPACE + "/settings",
                updatedWfsSettings,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());

        // Verify WFS settings were updated
        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/services/wfs/workspaces/"
                + WORKSPACE_ADMIN_WORKSPACE + "/settings.xml");
        assertEquals(200, response.getStatus());
        dom = dom(getBinaryInputStream(response));
        assertXpathEvaluatesTo("Updated WFS Title", "//title", dom);
        assertXpathEvaluatesTo("Updated Maintainer", "//maintainer", dom);

        // Test accessing unauthorized workspace service settings
        response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/services/wms/workspaces/" + WORKSPACE_NO_ACCESS + "/settings.xml");
        assertEquals(403, response.getStatus());
    }

    //
    // REST Index filtering tests
    //

    @Test
    public void testRestIndexFiltering() throws Exception {
        // Authenticate as workspace admin
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        // Request the REST API index
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH);
        assertEquals(200, response.getStatus());

        String content = response.getContentAsString();

        // Should include links the workspace admin can access
        assertThat(content, containsString("<a href=\"workspaces"));
        assertThat(content, containsString("<a href=\"namespaces"));
        assertThat(content, containsString("<a href=\"styles"));
        assertThat(content, containsString("<a href=\"templates"));

        // Should NOT include links the workspace admin cannot access
        assertThat(content, not(containsString("<a href=\"security")));
        assertThat(content, not(containsString("<a href=\"settings")));
    }

    //
    // Admin-only endpoint access tests
    //

    @Test
    public void testAdminOnlyEndpoints() throws Exception {
        // Define paths that only administrators should be able to access
        String[] adminOnlyPaths = {
            "/rest/security/roles",
            "/rest/security/masterpw",
            "/rest/security/usergroup/groups",
            "/rest/settings",
            "/rest/settings/contact",
            "/rest/urlchecks",
            "/rest/about/manifest",
            "/rest/about/status",
            "/rest/about/version"
        };

        // Test with admin user - should have access to all endpoints
        login("admin", "geoserver");

        for (String path : adminOnlyPaths) {
            MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + path);
            assertEquals("Admin should have access to " + path, 200, response.getStatus());
        }

        // Test with workspace admin user - should be denied access to all endpoints
        login(WORKSPACE_ADMIN_USER, WORKSPACE_ADMIN_PASSWORD);

        for (String path : adminOnlyPaths) {
            MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + path);
            assertEquals("Workspace admin should NOT have access to " + path, 403, response.getStatus());
        }
    }

    //
    // Helper methods
    //

    private void login(String username, String password) {
        logout();
        setRequestAuth(username, password);
    }
}
