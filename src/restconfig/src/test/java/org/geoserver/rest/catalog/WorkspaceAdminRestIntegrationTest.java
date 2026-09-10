/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Integration tests for the workspace administrator REST API functionality.
 *
 * <p>Uses the default test data workspaces: {@code cite} as the workspace the test user can administer, and {@code sf}
 * as a workspace without admin access, to verify that workspace administrators have appropriate access to REST API
 * endpoints according to the security rules defined in rest.workspaceadmin.properties.
 */
@TestSetup(run = TestSetupFrequency.ONCE)
public class WorkspaceAdminRestIntegrationTest extends WorkspaceAdminCatalogRESTTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // Create a workspace-specific style in cite for style tests
        Catalog cat = getCatalog();
        if (cat.getStyleByName(cat.getWorkspaceByName(WS), "citeStyle") == null) {
            StyleInfo style = cat.getFactory().createStyle();
            style.setName("citeStyle");
            style.setWorkspace(cat.getWorkspaceByName(WS));
            style.setFilename("citeStyle.sld");
            cat.add(style);
        }
    }

    //
    // Workspace access tests
    //

    @Test
    public void testListWorkspaces() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/workspaces.xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(response, true);
        // SecureCatalog filters: should only see administrable workspace
        assertXpathExists("//workspace[name='%s']".formatted(WS), dom);
        assertXpathNotExists("//workspace[name='%s']".formatted(WS_OTHER), dom);
    }

    @Test
    public void testGetWorkspaceWithAccess() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/workspaces/%s.xml".formatted(WS));
        assertEquals(200, response.getStatus());

        assertXpathEvaluatesTo(WS, "/workspace/name", dom(response, true));
    }

    @Test
    public void testGetWorkspaceWithoutAccess() throws Exception {
        setWorkspaceAdminRequestAuth();

        // SecureCatalog hides non-adminable workspaces -> 404
        MockHttpServletResponse response = getAsServletResponse("/rest/workspaces/%s.xml".formatted(WS_OTHER));
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testUpdateWorkspaceWithAccess() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<workspace><name>%s</name></workspace>".formatted(WS);
        MockHttpServletResponse response =
                putAsServletResponse("/rest/workspaces/" + WS, xml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRenameWorkspaceDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<workspace><name>renamed</name></workspace>";
        MockHttpServletResponse response =
                putAsServletResponse("/rest/workspaces/" + WS, xml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testUpdateWorkspaceWithoutAccess() throws Exception {
        setWorkspaceAdminRequestAuth();

        // SecureCatalog hides it -> 404
        String xml = "<workspace><name>%s</name></workspace>".formatted(WS_OTHER);
        MockHttpServletResponse response =
                putAsServletResponse("/rest/workspaces/" + WS_OTHER, xml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDeleteWorkspaceDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = deleteAsServletResponse("/rest/workspaces/" + WS);
        assertEquals(403, response.getStatus());
    }

    //
    // Namespace access tests
    //

    @Test
    public void testListNamespaces() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/namespaces.xml");
        assertEquals(200, response.getStatus());

        String content = response.getContentAsString();
        assertThat(content, containsString(WS));
    }

    @Test
    public void testGetNamespaceWithAccess() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/namespaces/%s.xml".formatted(WS));
        assertEquals(200, response.getStatus());

        assertXpathEvaluatesTo(WS, "/namespace/prefix", dom(response, true));
    }

    @Test
    public void testUpdateNamespaceWithAccess() throws Exception {
        setWorkspaceAdminRequestAuth();

        String originalUri = MockData.CITE_URI;
        String updatedUri = "http://updated-cite-uri";

        String xml = "<namespace><prefix>%s</prefix><uri>%s</uri></namespace>".formatted(WS, updatedUri);
        MockHttpServletResponse response =
                putAsServletResponse("/rest/namespaces/" + WS, xml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());

        // Verify
        response = getAsServletResponse("/rest/namespaces/%s.xml".formatted(WS));
        assertXpathEvaluatesTo(updatedUri, "/namespace/uri", dom(response, true));

        // Restore original URI
        xml = "<namespace><prefix>%s</prefix><uri>%s</uri></namespace>".formatted(WS, originalUri);
        putAsServletResponse("/rest/namespaces/" + WS, xml, MediaType.APPLICATION_XML_VALUE);
    }

    @Test
    public void testRenameNamespaceDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<namespace><prefix>renamed</prefix><uri>%s</uri></namespace>".formatted(MockData.CITE_URI);
        MockHttpServletResponse response =
                putAsServletResponse("/rest/namespaces/" + WS, xml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    //
    // Styles access tests
    //

    @Test
    public void testListGlobalStyles() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/styles.xml");
        assertEquals(200, response.getStatus());

        Document dom = dom(response, true);
        assertXpathExists("//style[name='generic']", dom);
        assertXpathExists("//style[name='line']", dom);
    }

    @Test
    public void testGetGlobalStyle() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/styles/line.xml");
        assertEquals(200, response.getStatus());
        assertXpathEvaluatesTo("line", "/style/name", dom(response, true));
    }

    @Test
    public void testUpdateGlobalStyleDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<style><name>line</name><filename>line.sld</filename></style>";
        MockHttpServletResponse response =
                putAsServletResponse("/rest/styles/line", xml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testCreateGlobalStyleDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        String xml = "<style><name>newGlobalStyle</name><filename>newGlobalStyle.sld</filename></style>";
        MockHttpServletResponse response = postAsServletResponse("/rest/styles", xml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testDeleteGlobalStyleDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = deleteAsServletResponse("/rest/styles/line");
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testWorkspaceSpecificStyles() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/workspaces/%s/styles.xml".formatted(WS));
        assertEquals(200, response.getStatus());

        Document dom = dom(response, true);
        assertXpathExists("//style[name='citeStyle']", dom);

        // Update workspace style (should be allowed)
        String xml = "<style><name>citeStyle</name><filename>citeStyle.sld</filename></style>";
        response = putAsServletResponse(
                "/rest/workspaces/%s/styles/citeStyle".formatted(WS), xml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());
    }

    //
    // Layer group access tests
    //

    @Test
    public void testGlobalLayerGroupsDenied() throws Exception {
        // Create a global layer group as admin
        login("admin", "geoserver");
        Catalog cat = getCatalog();
        if (cat.getLayerGroupByName("testGlobalLayerGroup") == null) {
            LayerGroupInfo lg = cat.getFactory().createLayerGroup();
            lg.setName("testGlobalLayerGroup");
            lg.getLayers().add(cat.getLayers().get(0));
            cat.add(lg);
        }

        setWorkspaceAdminRequestAuth();

        assertEquals(403, getAsServletResponse("/rest/layergroups.xml").getStatus());
        assertEquals(
                403,
                getAsServletResponse("/rest/layergroups/testGlobalLayerGroup.xml")
                        .getStatus());
        assertEquals(
                403,
                deleteAsServletResponse("/rest/layergroups/testGlobalLayerGroup")
                        .getStatus());
    }

    @Test
    public void testWorkspaceSpecificLayerGroups() throws Exception {
        setWorkspaceAdminRequestAuth();

        Catalog cat = getCatalog();
        LayerInfo layer = cat.getLayerByName(WS + ":" + CITE_LAYER);
        assertNotNull("Layer %s:%s not found".formatted(WS, CITE_LAYER), layer);

        // Create a workspace-specific layer group using an existing cite layer
        String createXml =
                """
                <layerGroup>
                  <name>wsLayerGroup</name>
                  <title>WS Layer Group</title>
                  <publishables>
                    <published type="layer"><name>%s:%s</name></published>
                  </publishables>
                  <styles><style><name>polygon</name></style></styles>
                </layerGroup>
                """
                        .formatted(WS, CITE_LAYER);

        MockHttpServletResponse response = postAsServletResponse(
                "/rest/workspaces/%s/layergroups".formatted(WS), createXml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(201, response.getStatus());

        // Verify it exists
        response = getAsServletResponse("/rest/workspaces/%s/layergroups/wsLayerGroup.xml".formatted(WS));
        assertEquals(200, response.getStatus());
        assertXpathEvaluatesTo("wsLayerGroup", "/layerGroup/name", dom(response, true));

        // Update it
        String updateXml =
                """
                <layerGroup>
                  <name>wsLayerGroup</name>
                  <title>Updated Title</title>
                </layerGroup>
                """;
        response = putAsServletResponse(
                "/rest/workspaces/%s/layergroups/wsLayerGroup".formatted(WS),
                updateXml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());

        // Verify update
        response = getAsServletResponse("/rest/workspaces/%s/layergroups/wsLayerGroup.xml".formatted(WS));
        assertXpathEvaluatesTo("Updated Title", "/layerGroup/title", dom(response, true));

        // POST to unauthorized workspace — SecureCatalog hides workspace -> 404
        response = postAsServletResponse(
                "/rest/workspaces/%s/layergroups".formatted(WS_OTHER), createXml, MediaType.APPLICATION_XML_VALUE);
        assertThat("Should deny in unauthorized workspace", response.getStatus(), Matchers.oneOf(403, 404));

        // Delete
        response = deleteAsServletResponse("/rest/workspaces/%s/layergroups/wsLayerGroup".formatted(WS));
        assertEquals(200, response.getStatus());
    }

    //
    // Templates access tests
    //

    @Test
    public void testListGlobalTemplates() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/templates.json");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testGetGlobalTemplate() throws Exception {
        setAdminRequestAuth();
        String templateContent = "This is a test template for ${name}";
        MockHttpServletResponse adminResponse =
                putAsServletResponse("/rest/templates/test_template.ftl", templateContent, MediaType.TEXT_PLAIN_VALUE);
        assertThat("Admin should be able to create template", adminResponse.getStatus(), Matchers.oneOf(200, 201));

        setWorkspaceAdminRequestAuth();
        MockHttpServletResponse response = getAsServletResponse("/rest/templates/test_template.ftl");
        assertEquals(200, response.getStatus());
        assertEquals(templateContent, response.getContentAsString());
    }

    @Test
    public void testUpdateGlobalTemplateDenied() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response =
                putAsServletResponse("/rest/templates/test_template.ftl", "updated", MediaType.TEXT_PLAIN_VALUE);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testWorkspaceSpecificTemplates() throws Exception {
        setWorkspaceAdminRequestAuth();

        String content = "This is a workspace template for ${name}";
        MockHttpServletResponse response = putAsServletResponse(
                "/rest/workspaces/%s/templates/ws_template.ftl".formatted(WS), content, MediaType.TEXT_PLAIN_VALUE);
        assertThat(response.getStatus(), Matchers.oneOf(200, 201));

        // Verify it exists
        response = getAsServletResponse("/rest/workspaces/%s/templates/ws_template.ftl".formatted(WS));
        assertEquals(200, response.getStatus());
        assertEquals(content, response.getContentAsString());

        // Try in unauthorized workspace — should be denied
        response = putAsServletResponse(
                "/rest/workspaces/%s/templates/ws_template.ftl".formatted(WS_OTHER),
                content,
                MediaType.TEXT_PLAIN_VALUE);
        assertThat(
                "Should deny template creation in unauthorized workspace",
                response.getStatus(),
                Matchers.oneOf(403, 404));
    }

    //
    // Layer access tests
    //

    @Test
    public void testListLayers() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/layers.xml");
        assertEquals(200, response.getStatus());

        String content = response.getContentAsString();
        // Should see cite layers, not sf layers
        assertThat(content, containsString(CITE_LAYER));
        assertThat(content, not(containsString(WS_OTHER + ":")));
    }

    @Test
    public void testCreateLayerInUnauthorizedWorkspace() throws Exception {
        setWorkspaceAdminRequestAuth();

        // SecureCatalog hides the sf workspace -> datastore not found -> 404
        String xml = "<featureType><name>testLayer</name><nativeName>testLayer</nativeName></featureType>";
        MockHttpServletResponse response = postAsServletResponse(
                "/rest/workspaces/%s/datastores/sf/featuretypes".formatted(WS_OTHER),
                xml,
                MediaType.APPLICATION_XML_VALUE);
        assertEquals(404, response.getStatus());
    }

    //
    // CRS endpoint (read-only)
    //

    @Test
    public void testCrsAccessible() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/crs");
        assertEquals(200, response.getStatus());
    }

    // REST Resource access tests are in ResourceControllerWorkspaceAdminTest

    //
    // Workspace-specific settings tests
    //

    @Test
    public void testWorkspaceSettings() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest/workspaces/%s/settings.xml".formatted(WS));
        assertEquals(200, response.getStatus());

        String settingsXml =
                """
                <settings>
                  <contact><addressCity>TestCity</addressCity></contact>
                  <charset>UTF-8</charset>
                  <numDecimals>8</numDecimals>
                  <verbose>false</verbose>
                  <verboseExceptions>false</verboseExceptions>
                </settings>
                """;
        response = putAsServletResponse(
                "/rest/workspaces/%s/settings".formatted(WS), settingsXml, MediaType.APPLICATION_XML_VALUE);
        assertEquals(200, response.getStatus());

        // Verify
        response = getAsServletResponse("/rest/workspaces/%s/settings.xml".formatted(WS));
        Document dom = dom(response, true);
        assertXpathEvaluatesTo("TestCity", "//contact/addressCity", dom);

        // SecureCatalog hides the workspace -> LocalSettingsController returns 404
        response = getAsServletResponse("/rest/workspaces/%s/settings.xml".formatted(WS_OTHER));
        assertEquals(404, response.getStatus());
    }

    //
    // REST Index filtering tests
    //

    @Test
    public void testRestIndexFiltering() throws Exception {
        setWorkspaceAdminRequestAuth();

        MockHttpServletResponse response = getAsServletResponse("/rest");
        assertEquals(200, response.getStatus());

        String content = response.getContentAsString();
        assertThat(content, containsString("workspaces"));
        assertThat(content, containsString("namespaces"));
        assertThat(content, containsString("styles"));
        assertThat(content, containsString("templates"));

        assertThat(content, not(containsString("href=\"security")));
    }

    //
    // Admin-only endpoint access tests
    //

    @Test
    public void testAdminOnlyEndpoints() throws Exception {
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

        setAdminRequestAuth();
        for (String path : adminOnlyPaths) {
            assertEquals(
                    "Admin should have access to " + path,
                    200,
                    getAsServletResponse(path).getStatus());
        }

        setWorkspaceAdminRequestAuth();
        for (String path : adminOnlyPaths) {
            assertEquals(
                    "WS admin should NOT have access to " + path,
                    403,
                    getAsServletResponse(path).getStatus());
        }
    }
}
