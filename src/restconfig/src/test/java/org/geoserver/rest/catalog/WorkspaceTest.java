/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.rest.RestBaseController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WorkspaceTest extends CatalogRESTTestSupport {

    @Before
    public void addWorkspaces() {
        getTestData()
                .addWorkspace(SystemTestData.DEFAULT_PREFIX, SystemTestData.DEFAULT_URI, catalog);
        getTestData().addWorkspace(SystemTestData.SF_PREFIX, SystemTestData.SF_URI, catalog);
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(
                catalog.getNamespaces().size(), dom.getElementsByTagName("workspace").getLength());
        NodeList nodes = dom.getElementsByTagName("workspace");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            String nodeValue = node.getTextContent().trim();

            assertNotNull(catalog.getWorkspaceByName(nodeValue));
        }
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.html");

        List<WorkspaceInfo> workspaces = catalog.getWorkspaces();

        NodeList links = xp.getMatchingNodes("//html:a", dom);
        assertEquals(workspaces.size(), links.getLength());

        for (int i = 0; i < workspaces.size(); i++) {
            WorkspaceInfo ws = workspaces.get(i);
            Element link = (Element) links.item(i);

            assertTrue(link.getAttribute("href").endsWith(ws.getName() + ".html"));
        }
    }

    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals(
                405,
                putAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces").getStatus());
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces").getStatus());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf.xml");
        assertEquals("workspace", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());

        Element name = (Element) dom.getElementsByTagName("name").item(0);
        assertEquals("sf", name.getFirstChild().getTextContent());

        Element datastores = (Element) dom.getElementsByTagName("dataStores").item(0);
        assertNotNull(datastores);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        List<StoreInfo> stores = catalog.getStoresByWorkspace("sf", StoreInfo.class);

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf.html");

        NodeList links = xp.getMatchingNodes("//html:a", dom);
        assertEquals(stores.size(), links.getLength());

        for (int i = 0; i < stores.size(); i++) {
            StoreInfo store = stores.get(i);
            Element link = (Element) links.item(i);

            assertTrue(link.getAttribute("href").endsWith(store.getName() + ".html"));
        }
    }

    @Test
    public void testGetWrongWorkspace() throws Exception {
        // Parameters for the request
        String workspace = "sfsssss";
        // Request path
        String requestPath = RestBaseController.ROOT_PATH + "/workspaces/" + workspace + ".html";
        // Exception path
        String exception = "No such workspace: '" + workspace + "'";
        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(exception));
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(exception));
    }

    @Test
    public void testGetNonExistant() throws Exception {
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/none")
                        .getStatus());
    }

    @Test
    public void testPostAsXML() throws Exception {
        String xml = "<workspace>" + "<name>foo</name>" + "</workspace>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces", xml, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        // System.out.println(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/foo"));

        WorkspaceInfo ws = getCatalog().getWorkspaceByName("foo");
        assertNotNull(ws);
        assertNotNull(ws.getDateCreated());
        // check corresponding namespace creation
        NamespaceInfo ns = getCatalog().getNamespaceByPrefix("foo");
        assertNotNull(ns);

        removeWorkspace("foo");
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf.json");
        JSONObject workspace = ((JSONObject) json).getJSONObject("workspace");
        assertEquals("sf", workspace.get("name"));
        assertNotNull(workspace.get("dataStores"));
    }

    @Test
    public void testPostAsJSON() throws Exception {
        removeWorkspace("foo");
        String json = "{'workspace':{ 'name':'foo' }}";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces", json, "text/json");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/foo"));

        WorkspaceInfo ws = getCatalog().getWorkspaceByName("foo");
        assertNotNull(ws);
        assertNotNull(ws.getDateCreated());
    }

    @Test
    public void testPostToResource() throws Exception {
        String xml = "<workspace>" + "<name>changed</name>" + "</workspace>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs", xml, "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/newExistant")
                        .getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        String xml = "<workspace>" + "<name>foo</name>" + "</workspace>";
        post(RestBaseController.ROOT_PATH + "/workspaces", xml);

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/foo.xml");
        assertEquals("workspace", dom.getDocumentElement().getNodeName());

        assertEquals(
                200,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/foo")
                        .getStatus());
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/foo.xml")
                        .getStatus());
    }

    @Test
    public void testDeleteNonEmptyForbidden() throws Exception {
        getTestData().addVectorLayer(SystemTestData.PRIMITIVEGEOFEATURE, catalog);
        assertEquals(
                403,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf")
                        .getStatus());
    }

    @Test
    public void testDeleteDefault() throws Exception {
        assertEquals(
                200,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/default")
                        .getStatus());
    }

    @Test
    public void testDeleteAllOneByOne() throws Exception {
        for (WorkspaceInfo ws : getCatalog().getWorkspaces()) {
            // empty the workspace otherwise we can't remove it
            CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(getCatalog());
            for (StoreInfo store : getCatalog().getStoresByWorkspace(ws, StoreInfo.class)) {
                store.accept(visitor);
            }

            // actually go and remove the store
            String resource = RestBaseController.ROOT_PATH + "/workspaces/" + ws.getName();
            assertEquals(200, deleteAsServletResponse(resource).getStatus());
            assertEquals(404, getAsServletResponse(resource).getStatus());
        }
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces.xml");
        assertEquals(0, dom.getElementsByTagName("workspace").getLength());
    }

    @Test
    public void testDeleteRecursive() throws Exception {
        getTestData().addVectorLayer(SystemTestData.PRIMITIVEGEOFEATURE, catalog);
        List<StoreInfo> stores = catalog.getStoresByWorkspace("sf", StoreInfo.class);
        assertFalse(stores.isEmpty());

        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf?recurse=true");
        assertEquals(200, response.getStatus());

        assertNull(catalog.getWorkspaceByName("sf"));
        assertNull(catalog.getNamespaceByPrefix("sf"));

        for (StoreInfo s : stores) {
            assertNull(catalog.getStoreByName(s.getName(), StoreInfo.class));
        }
    }

    @Test
    public void testPut() throws Exception {
        String xml =
                "<workspace>"
                        + "<metadata>"
                        + "<foo>"
                        + "<string>bar</string>"
                        + "</foo>"
                        + "</metadata>"
                        + "</workspace>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs", xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/gs.xml");
        assertXpathEvaluatesTo("1", "count(//name[text()='gs'])", dom);
        assertXpathEvaluatesTo("1", "count(//entry[@key='foo' and text()='bar'])", dom);
    }

    @Test
    public void testPutNameChangeForbidden() throws Exception {
        String xml = "<workspace>" + "<name></name>" + "</workspace>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs", xml, "text/xml");
        assertEquals(403, response.getStatus());

        String json = "{'workspace':{ 'name': '' }}";
        response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs", json, "application/json");
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testPutNameChange() throws Exception {
        String xml = "<workspace>" + "<name>changed</name>" + "</workspace>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs", xml, "text/xml");
        assertEquals(200, response.getStatus());

        // verify if changed
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/changed.json");
        JSONObject workspace = ((JSONObject) json).getJSONObject("workspace");
        assertEquals("changed", workspace.get("name"));

        // undo name change -- this workspace is needed by other tests

        xml = "<workspace>" + "<name>gs</name>" + "</workspace>";

        response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/changed", xml, "text/xml");
        assertEquals(200, response.getStatus());

        // verify if changed
        json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/gs.json");
        workspace = ((JSONObject) json).getJSONObject("workspace");
        assertEquals("gs", workspace.get("name"));
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml =
                "<workspace>"
                        + "<metadata>"
                        + "<entry>"
                        + "<string>foo</string>"
                        + "<string>bar</string>"
                        + "</entry>"
                        + "</metadata>"
                        + "</workspace>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/nonExistant", xml, "text/xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetDefaultWorkspace() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/default.xml");

        assertEquals("workspace", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());
    }

    @Test
    public void testPutDefaultWorkspace() throws Exception {
        WorkspaceInfo def = getCatalog().getDefaultWorkspace();
        assertEquals("gs", def.getName());

        String json = "{'workspace':{ 'name':'sf' }}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/default", json, "text/json");
        assertEquals(200, response.getStatus());

        def = getCatalog().getDefaultWorkspace();
        assertEquals("sf", def.getName());
    }

    @Test
    public void testRoundTripXMLSerialization() throws Exception {
        // we can do this round trip two ways - first upload/download and check
        removeWorkspace("ian");
        String xml = "<workspace>" + "<name>foo</name>" + "</workspace>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces", xml, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/foo"));

        WorkspaceInfo ws = getCatalog().getWorkspaceByName("foo");
        assertNotNull(ws);
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/foo.xml");
        assertEquals("workspace", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());

        Element name = (Element) dom.getElementsByTagName("name").item(0);
        assertEquals("foo", name.getFirstChild().getTextContent());

        // second download/upload - this runs into GEOS-5603(?)
        dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/"
                                + SystemTestData.SF_PREFIX
                                + ".xml");
        name = (Element) dom.getElementsByTagName("name").item(0);

        name.setTextContent("ian");
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(dom), new StreamResult(writer));
        String output = writer.getBuffer().toString();

        response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces", output, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/ian"));
    }

    @Test
    public void testRoundTripJSONSerialization() throws Exception {
        // we can do this round trip two ways - first upload/download and check
        removeWorkspace("ian");
        String json = "{'workspace':{'name':'foo'}}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces", json, "application/json");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/foo"));

        WorkspaceInfo ws = getCatalog().getWorkspaceByName("foo");
        assertNotNull(ws);
        JSON jsonObj = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/foo.json");
        JSONObject workspace = ((JSONObject) jsonObj).getJSONObject("workspace");
        assertEquals("foo", workspace.get("name"));

        // second download/upload - this runs into GEOS-5603(?)
        jsonObj =
                getAsJSON(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/"
                                + SystemTestData.SF_PREFIX
                                + ".json");
        workspace = ((JSONObject) jsonObj).getJSONObject("workspace");

        workspace.put("name", "ian");

        String output = jsonObj.toString();

        response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces", output, "application/json");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/ian"));
    }

    @Test
    public void testIsolatedWorkspaceHandling() throws Exception {
        // create an isolated workspace
        String xmlPost =
                "<workspace>"
                        + "  <name>isolated_workspace</name>"
                        + "  <isolated>true</isolated>"
                        + "</workspace>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces.xml", xmlPost, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        // check hat the created workspace is isolated
        WorkspaceInfo workspace = getCatalog().getWorkspaceByName("isolated_workspace");
        assertThat(workspace, notNullValue());
        assertThat(workspace.isIsolated(), is(true));
        assertNotNull(workspace.getDateCreated());
        // check that the created namespace is isolated
        NamespaceInfo namespace = getCatalog().getNamespaceByPrefix("isolated_workspace");
        assertThat(namespace, notNullValue());
        assertThat(namespace.isIsolated(), is(true));
        // make the workspace non isolated
        String xmlPut =
                "<workspace>"
                        + "  <name>isolated_workspace</name>"
                        + "  <isolated>false</isolated>"
                        + "</workspace>";
        response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/isolated_workspace",
                        xmlPut,
                        "text/xml");
        assertEquals(200, response.getStatus());
        // check that the workspace was correctly updated
        workspace = getCatalog().getWorkspaceByName("isolated_workspace");
        assertThat(workspace, notNullValue());
        assertThat(workspace.isIsolated(), is(false));
        assertNotNull(workspace.getDateModified());
        // check that the namespace was correctly updated
        namespace = getCatalog().getNamespaceByPrefix("isolated_workspace");
        assertThat(namespace, notNullValue());
        assertThat(namespace.isIsolated(), is(false));
    }
}
