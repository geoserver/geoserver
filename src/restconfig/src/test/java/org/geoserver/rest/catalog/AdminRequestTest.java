/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.AccessMode;
import org.geoserver.security.AdminRequest;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class AdminRequestTest extends CatalogRESTTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("global");
        lg.getLayers().add(catalog.getLayerByName("sf:PrimitiveGeoFeature"));
        lg.getLayers().add(catalog.getLayerByName("sf:AggregateGeoFeature"));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));
        catalog.add(lg);

        lg = catalog.getFactory().createLayerGroup();
        lg.setName("local");
        lg.setWorkspace(catalog.getWorkspaceByName("sf"));
        lg.getLayers().add(catalog.getLayerByName("sf:PrimitiveGeoFeature"));
        lg.getLayers().add(catalog.getLayerByName("sf:AggregateGeoFeature"));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.getStyles().add(catalog.getStyleByName(StyleInfo.DEFAULT_POINT));
        lg.setBounds(new ReferencedEnvelope(-180, -90, 180, 90, CRS.decode("EPSG:4326")));
        catalog.add(lg);

        Catalog cat = getCatalog();

        // add two workspace specific styles
        StyleInfo s = cat.getFactory().createStyle();
        s.setName("sf_style");
        s.setWorkspace(cat.getWorkspaceByName("sf"));
        s.setFilename("sf.sld");
        cat.add(s);

        s = cat.getFactory().createStyle();
        s.setName("cite_style");
        s.setWorkspace(cat.getWorkspaceByName("cite"));
        s.setFilename("cite.sld");
        cat.add(s);

        addUser("cite", "cite", null, Collections.singletonList("ROLE_CITE_ADMIN"));
        addUser("sf", "sf", null, Collections.singletonList("ROLE_SF_ADMIN"));

        addLayerAccessRule("*", "*", AccessMode.ADMIN, "ROLE_ADMINISTRATOR");
        addLayerAccessRule("cite", "*", AccessMode.ADMIN, "ROLE_CITE_ADMIN");
        addLayerAccessRule("sf", "*", AccessMode.ADMIN, "ROLE_SF_ADMIN");
    }

    @After
    public void clearAdminRequest() {
        AdminRequest.finish();
    }

    @Override
    public void login() {
        // skip the login by default
    }

    void loginAsCite() {
        login("cite", "cite", "ROLE_CITE_ADMIN");
    }

    void loginAsSf() {
        login("sf", "sf", "ROLE_SF_ADMIN");
    }

    @Test
    public void testWorkspaces() throws Exception {
        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces.xml")
                        .getStatus());
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(0, dom.getElementsByTagName("workspace").getLength());

        super.login();
        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(
                getCatalog().getWorkspaces().size(),
                dom.getElementsByTagName("workspace").getLength());

        loginAsCite();
        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces.xml")
                        .getStatus());
        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(1, dom.getElementsByTagName("workspace").getLength());
    }

    @Test
    public void testWorkspacesWithProxyHeaders() throws Exception {
        GeoServerInfo ginfo = getGeoServer().getGlobal();
        SettingsInfo settings = getGeoServer().getGlobal().getSettings();
        settings.setUseHeadersProxyURL(true);
        settings.setProxyBaseUrl(
                "${X-Forwarded-Proto}://${X-Forwarded-Host}/${X-Forwarded-Path} ${X-Forwarded-Proto}://${X-Forwarded-Host}");
        ginfo.setSettings(settings);
        getGeoServer().save(ginfo);
        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces.xml")
                        .getStatus());
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(0, dom.getElementsByTagName("workspace").getLength());

        super.login();
        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(
                getCatalog().getWorkspaces().size(),
                dom.getElementsByTagName("workspace").getLength());

        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces.xml")
                        .getStatus());
        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(
                getCatalog().getWorkspaces().size(),
                dom.getElementsByTagName("workspace").getLength());
    }

    @Test
    public void testWorkspace() throws Exception {
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf.xml")
                        .getStatus());
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/cite.xml")
                        .getStatus());

        loginAsCite();
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf.xml")
                        .getStatus());
        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/cite.xml")
                        .getStatus());
    }

    @Test
    public void testGlobalLayerGroupReadOnly() throws Exception {
        loginAsSf();

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups.xml");
        assertEquals(1, dom.getElementsByTagName("layerGroup").getLength());
        assertXpathEvaluatesTo("global", "//layerGroup/name", dom);

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups/global.xml");
        assertEquals("layerGroup", dom.getDocumentElement().getNodeName());

        String xml = "<layerGroup>"
                + "<styles>"
                + "<style>polygon</style>"
                + "<style>line</style>"
                + "</styles>"
                + "</layerGroup>";

        MockHttpServletResponse response =
                putAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups/global", xml, "text/xml");
        assertEquals(405, response.getStatus());

        xml = "<layerGroup>"
                + "<name>newLayerGroup</name>"
                + "<layers>"
                + "<layer>Ponds</layer>"
                + "<layer>Forests</layer>"
                + "</layers>"
                + "<styles>"
                + "<style>polygon</style>"
                + "<style>point</style>"
                + "</styles>"
                + "</layerGroup>";
        response = postAsServletResponse(RestBaseController.ROOT_PATH + "/layergroups", xml, "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testLocalLayerGroupHidden() throws Exception {
        loginAsSf();

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups.xml");
        print(dom);
        assertEquals(1, dom.getElementsByTagName("layerGroup").getLength());
        assertXpathEvaluatesTo("global", "//layerGroup/name", dom);

        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/cite/layergroups.xml");
        assertEquals(404, response.getStatus());

        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/cite/layergroups.xml");
        assertEquals(404, response.getStatus());

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/layergroups.xml");
        assertEquals(1, dom.getElementsByTagName("layerGroup").getLength());
        assertXpathEvaluatesTo("global", "//layerGroup/name", dom);

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/layergroups.xml");
        assertEquals(1, dom.getElementsByTagName("layerGroup").getLength());
        assertXpathEvaluatesTo("local", "//layerGroup/name", dom);
    }

    @Test
    public void testGlobalStyleReadOnly() throws Exception {
        loginAsSf();

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles.xml");

        assertXpathNotExists("//style/name[text() = 'sf_style']", dom);
        assertXpathNotExists("//style/name[text() = 'cite_style']", dom);

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles/point.xml");
        assertEquals("style", dom.getDocumentElement().getNodeName());

        String xml = "<style>" + "<filename>foo.sld</filename>" + "</style>";

        MockHttpServletResponse response =
                putAsServletResponse(RestBaseController.ROOT_PATH + "/styles/point", xml, "text/xml");
        assertEquals(405, response.getStatus());

        xml = "<style>" + "<name>foo</name>" + "<filename>foo.sld</filename>" + "</style>";
        response = postAsServletResponse(RestBaseController.ROOT_PATH + "/styles", xml, "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testLocalStyleHidden() throws Exception {
        loginAsCite();

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles.xml");
        assertXpathNotExists("//style/name[text() = 'cite_style']", dom);
        assertXpathNotExists("//style/name[text() = 'sf_style']", dom);

        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf/styles.xml");
        assertEquals(404, response.getStatus());

        loginAsSf();

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles.xml");

        assertXpathNotExists("//style/name[text() = 'cite_style']", dom);
        assertXpathNotExists("//style/name[text() = 'sf_style']", dom);

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/styles.xml");
        assertEquals(1, dom.getElementsByTagName("style").getLength());
        assertXpathEvaluatesTo("sf_style", "//style/name", dom);
    }

    @Test
    public void testReadOnlyUserRestCatalogVisibility() throws Exception {

        // Create a user with READ permission but no ADMIN permission.
        addUser("readonly", "readonly", null, Collections.singletonList("ROLE_CITE_READER"));

        // Explicitly grant the user READ access to the cite workspace.
        addLayerAccessRule("cite", "*", AccessMode.READ, "ROLE_CITE_READER");

        // Authenticate as our read-only user.
        login("readonly", "readonly", "ROLE_CITE_READER");

        // The REST request is accepted.
        assertEquals(
                200,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces.xml")
                        .getStatus());

        // Current behavior: no workspaces are returned.
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");

        assertEquals(0, dom.getElementsByTagName("workspace").getLength());

        // Current behavior: an individual workspace is also hidden.
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/cite.xml")
                        .getStatus());
    }

    @Test
    public void testReadOnlyMetadataWorkspaceDiscovery() throws Exception {

        addUser("metadatareader", "metadatareader", null, List.of("ROLE_CITE_READER", "ROLE_METADATA_READER"));

        addLayerAccessRule("cite", "*", AccessMode.READ, "ROLE_CITE_READER");

        // Restrict sf to a role our metadata reader does not have.
        DataAccessRuleDAO dao = DataAccessRuleDAO.get();
        DataAccessRule sfRule = new DataAccessRule("sf", "*", AccessMode.READ, "ROLE_SF_READER");

        org.junit.Assert.assertTrue(dao.addRule(sfRule));
        dao.storeRules();

        try {
            login("metadatareader", "metadatareader", "ROLE_CITE_READER", "ROLE_METADATA_READER");

            MockHttpServletResponse response =
                    getAsServletResponse(RestBaseController.ROOT_PATH + "/metadata/workspaces.json");

            assertEquals(200, response.getStatus());

            String json = response.getContentAsString();

            // Authorized workspace must be visible.
            org.junit.Assert.assertTrue("Expected cite workspace: " + json, json.contains("\"cite\""));

            // Unauthorized workspace must not be visible.
            org.junit.Assert.assertFalse("Unauthorized sf workspace exposed: " + json, json.contains("\"sf\""));

        } finally {
            // Restore the original security rules after this test.
            dao.removeRule(sfRule);
            dao.storeRules();
        }
    }

    @Test
    public void testMetadataEndpointHasNoHardcodedRoleRequirement() throws Exception {

        // Create a user who does not have metadata API privileges.
        addUser("nometa", "nometa", null, Collections.singletonList("ROLE_NO_METADATA"));

        // Authenticate as the unprivileged user.
        login("nometa", "nometa", "ROLE_NO_METADATA");

        // Attempt to access our new metadata endpoint.
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/metadata/workspaces.json");

        // An unprivileged user must not be allowed to access this endpoint.
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testAnonymousMetadataAccessDenied() throws Exception {

        // Ensure no authentication is present.
        logout();
        setRequestAuth(null, null);

        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/metadata/workspaces.json");

        assertEquals(401, response.getStatus());

        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/metadata/layers.json");

        assertEquals(401, response.getStatus());
    }

    @Test
    public void testReadOnlyMetadataLayerDiscovery() throws Exception {

        addUser("layerreader", "layerreader", null, List.of("ROLE_CITE_READER", "ROLE_METADATA_READER"));

        // Allow reading layers in cite.
        addLayerAccessRule("cite", "*", AccessMode.READ, "ROLE_CITE_READER");

        DataAccessRuleDAO dao = DataAccessRuleDAO.get();

        // Deny access to the sf workspace.
        DataAccessRule sfRule = new DataAccessRule("sf", "*", AccessMode.READ, "ROLE_SF_READER");

        // Deny access to one specific layer in cite.
        DataAccessRule forestRule = new DataAccessRule("cite", "Forests", AccessMode.READ, "ROLE_FOREST_READER");

        org.junit.Assert.assertTrue(dao.addRule(sfRule));
        org.junit.Assert.assertTrue(dao.addRule(forestRule));

        dao.storeRules();

        try {

            login("layerreader", "layerreader", "ROLE_CITE_READER", "ROLE_METADATA_READER");

            MockHttpServletResponse response =
                    getAsServletResponse(RestBaseController.ROOT_PATH + "/metadata/layers.json");

            assertEquals(200, response.getStatus());

            String json = response.getContentAsString();

            // An authorized layer must appear.
            org.junit.Assert.assertTrue("Expected cite:Bridges: " + json, json.contains("\"cite:Bridges\""));

            // An unauthorized workspace must not appear.
            org.junit.Assert.assertFalse(
                    "Unauthorized sf layer exposed: " + json, json.contains("\"sf:PrimitiveGeoFeature\""));

            // A specifically restricted layer must not appear.
            org.junit.Assert.assertFalse(
                    "Unauthorized cite:Forests exposed: " + json, json.contains("\"cite:Forests\""));

        } finally {

            // Restore the original security configuration.
            dao.removeRule(sfRule);
            dao.removeRule(forestRule);
            dao.storeRules();
        }
    }

    @Test
    public void testReadOnlyMetadataFeatureType() throws Exception {

        addUser("schemareader", "schemareader", null, List.of("ROLE_CITE_READER", "ROLE_METADATA_READER"));

        addLayerAccessRule("cite", "*", AccessMode.READ, "ROLE_CITE_READER");

        DataAccessRuleDAO dao = DataAccessRuleDAO.get();

        // Restrict the entire sf workspace.
        DataAccessRule sfRule = new DataAccessRule("sf", "*", AccessMode.READ, "ROLE_SF_READER");

        // Restrict one particular layer within cite.
        DataAccessRule forestRule = new DataAccessRule("cite", "Forests", AccessMode.READ, "ROLE_FOREST_READER");

        org.junit.Assert.assertTrue(dao.addRule(sfRule));
        org.junit.Assert.assertTrue(dao.addRule(forestRule));
        dao.storeRules();

        try {
            login("schemareader", "schemareader", "ROLE_CITE_READER", "ROLE_METADATA_READER");

            // Authorized feature type.
            MockHttpServletResponse response = getAsServletResponse(
                    RestBaseController.ROOT_PATH + "/metadata/workspaces/cite/featuretypes/Bridges.json");

            assertEquals(200, response.getStatus());

            String json = response.getContentAsString();

            org.junit.Assert.assertTrue("Expected workspace name: " + json, json.contains("\"workspace\":\"cite\""));

            org.junit.Assert.assertTrue("Expected feature type name: " + json, json.contains("\"name\":\"Bridges\""));

            org.junit.Assert.assertTrue("Expected attributes: " + json, json.contains("\"attributes\""));

            // Datastore configuration must not be serialized.
            org.junit.Assert.assertFalse(
                    "Datastore configuration exposed: " + json, json.contains("connectionParameters"));

            // Layer-specific READ restriction.
            response = getAsServletResponse(
                    RestBaseController.ROOT_PATH + "/metadata/workspaces/cite/featuretypes/Forests.json");

            assertEquals(404, response.getStatus());

            // Workspace-level READ restriction.
            response = getAsServletResponse(RestBaseController.ROOT_PATH
                    + "/metadata/workspaces/sf/featuretypes/"
                    + "PrimitiveGeoFeature.json");

            assertEquals(404, response.getStatus());

        } finally {
            dao.removeRule(sfRule);
            dao.removeRule(forestRule);
            dao.storeRules();
        }
    }

    @Test
    public void testMetadataEndpointAllowsAdministrator() throws Exception {

        // Simulate normal administrator authentication.
        // This includes both the local administrator role
        // and GeoServer's system administrator authority.
        loginAsAdmin();

        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/metadata/workspaces.json");

        assertEquals(200, response.getStatus());
    }
}
