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
import static org.junit.Assert.assertThat;

import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.rest.RestBaseController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NamespaceTest extends CatalogRESTTestSupport {

    @Before
    public void cleanNamespaces() {
        removeWorkspace("foo");
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/namespaces.xml", 200);
        assertEquals(
                catalog.getNamespaces().size(), dom.getElementsByTagName("namespace").getLength());
    }

    @Test
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/namespaces.json");
        assertTrue(json instanceof JSONObject);

        JSONArray namespaces =
                ((JSONObject) json).getJSONObject("namespaces").getJSONArray("namespace");
        assertNotNull(namespaces);

        assertEquals(catalog.getNamespaces().size(), namespaces.size());
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/namespaces.html");

        List<NamespaceInfo> namespaces = catalog.getNamespaces();

        NodeList links = xp.getMatchingNodes("//html:a", dom);
        assertEquals(namespaces.size(), links.getLength());

        for (int i = 0; i < namespaces.size(); i++) {
            NamespaceInfo ws = namespaces.get(i);
            Element link = (Element) links.item(i);

            assertTrue(link.getAttribute("href").endsWith(ws.getPrefix() + ".html"));
        }
    }

    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals(
                405,
                putAsServletResponse(RestBaseController.ROOT_PATH + "/namespaces").getStatus());
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/namespaces").getStatus());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/namespaces/sf.xml", 200);
        assertEquals("namespace", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("prefix").getLength());

        Element prefix = (Element) dom.getElementsByTagName("prefix").item(0);
        assertEquals("sf", prefix.getFirstChild().getTextContent());

        Element name = (Element) dom.getElementsByTagName("uri").item(0);
        assertEquals(MockData.SF_URI, name.getFirstChild().getTextContent());
    }

    @Test
    public void testRoundTripXMLSerialization() throws Exception {
        removeNamespace("ian");
        String xml =
                "<namespace>"
                        + "<prefix>ian</prefix>"
                        + "<uri>http://ian.com</uri>"
                        + "</namespace>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/namespaces", xml, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/namespaces/ian"));

        NamespaceInfo ws = getCatalog().getNamespaceByPrefix("ian");
        assertNotNull(ws);
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/namespaces/ian.xml");
        assertEquals("namespace", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("prefix").getLength());

        Element prefix = (Element) dom.getElementsByTagName("prefix").item(0);
        assertEquals("ian", prefix.getFirstChild().getTextContent());

        Element name = (Element) dom.getElementsByTagName("uri").item(0);
        assertEquals("http://ian.com", name.getFirstChild().getTextContent());
    }

    @Test
    public void testGetAsHTML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/namespaces/sf.html");

        List<ResourceInfo> resources = catalog.getResourcesByNamespace("sf", ResourceInfo.class);
        NodeList listItems = xp.getMatchingNodes("//html:li", dom);
        assertEquals(resources.size(), listItems.getLength());

        for (int i = 0; i < resources.size(); i++) {
            ResourceInfo resource = resources.get(i);
            Element listItem = (Element) listItems.item(i);

            assertTrue(listItem.getFirstChild().getNodeValue().endsWith(resource.getName()));
        }
    }

    @Test
    public void testGetWrongNamespace() throws Exception {
        // Parameters for the request
        String namespace = "sfsssss";
        // Request path
        String requestPath = RestBaseController.ROOT_PATH + "/namespaces/" + namespace + ".html";
        // Exception path
        String exception = "No such namespace: '" + namespace + "'";
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
                getAsServletResponse(RestBaseController.ROOT_PATH + "/namespaces/none")
                        .getStatus());
    }

    @Test
    public void testPostAsXML() throws Exception {
        String xml =
                "<namespace>"
                        + "<prefix>foo</prefix>"
                        + "<uri>http://foo.com</uri>"
                        + "</namespace>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/namespaces", xml, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/namespaces/foo"));

        NamespaceInfo ns = getCatalog().getNamespaceByPrefix("foo");
        assertNotNull(ns);

        // check the corresponding workspace has been created
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("foo");
        assertNotNull(ws);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/namespaces/sf.json");
        JSONObject namespace = ((JSONObject) json).getJSONObject("namespace");
        assertEquals("sf", namespace.get("prefix"));
        assertEquals(MockData.SF_URI, namespace.get("uri"));
    }

    @Test
    public void testPostAsJSON() throws Exception {
        removeNamespace("foo");
        String json = "{'namespace':{ 'prefix':'foo', 'uri':'http://foo.com' }}";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/namespaces", json, "text/json");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/namespaces/foo"));

        NamespaceInfo ws = getCatalog().getNamespaceByPrefix("foo");
        assertNotNull(ws);
    }

    @Test
    public void testPostToResource() throws Exception {
        String xml = "<namespace>" + "<name>changed</name>" + "</namespace>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/namespaces/gs", xml, "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/namespaces/newExistant")
                        .getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        String xml =
                "<namespace>"
                        + "<prefix>foo</prefix>"
                        + "<uri>http://foo.com</uri>"
                        + "</namespace>";
        post(RestBaseController.ROOT_PATH + "/namespaces", xml);

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/namespaces/foo.xml");
        assertEquals("namespace", dom.getDocumentElement().getNodeName());

        assertEquals(
                200,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/namespaces/foo")
                        .getStatus());
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/namespaces/foo.xml")
                        .getStatus());
        // verify associated workspace was deleted
        assertEquals(
                404,
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/foo.xml")
                        .getStatus());
    }

    @Test
    public void testDeleteNonEmpty() throws Exception {
        assertEquals(
                401,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/namespaces/sf")
                        .getStatus());
    }

    @Test
    public void testPut() throws Exception {
        String xml = "<namespace>" + "<uri>http://changed</uri>" + "</namespace>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/namespaces/gs", xml, "text/xml");
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/namespaces/gs.xml");
        assertXpathEvaluatesTo("1", "count(//namespace/uri[text()='http://changed'])", dom);
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = "<namespace>" + "<name>changed</name>" + "</namespace>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/namespaces/nonExistant", xml, "text/xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGetDefaultNamespace() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/namespaces/default.xml");

        assertEquals("namespace", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("prefix").getLength());
        assertEquals(1, dom.getElementsByTagName("uri").getLength());
    }

    @Test
    public void testPutDefaultNamespace() throws Exception {
        NamespaceInfo def = getCatalog().getDefaultNamespace();
        assertEquals("gs", def.getPrefix());

        String json = "{'namespace':{ 'prefix':'sf' }}";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/namespaces/default", json, "text/json");
        assertEquals(200, response.getStatus());

        def = getCatalog().getDefaultNamespace();
        assertEquals("sf", def.getPrefix());
    }

    @Test
    public void testIsolatedNamespacesHandling() throws Exception {
        // create an isolated namespace
        String xmlPost =
                "<namespace>"
                        + "  <id>isolated_namespace</id>"
                        + "  <prefix>isolated_prefix</prefix>"
                        + "  <uri>http://www.isolated.org/1.0</uri>"
                        + "  <isolated>true</isolated>"
                        + "</namespace>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/namespaces.xml", xmlPost, "text/xml");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        // check that the created namespace is isolated
        NamespaceInfo namespace = getCatalog().getNamespaceByPrefix("isolated_prefix");
        assertThat(namespace, notNullValue());
        assertThat(namespace.isIsolated(), is(true));
        // check hat the created workspace is isolated
        WorkspaceInfo workspace = getCatalog().getWorkspaceByName("isolated_prefix");
        assertThat(workspace, notNullValue());
        assertThat(workspace.isIsolated(), is(true));
        // make the namespace non isolated
        String xmlPut =
                "<namespace>"
                        + "  <id>isolated_namespace</id>"
                        + "  <prefix>isolated_prefix</prefix>"
                        + "  <uri>http://www.isolated.org/1.0</uri>"
                        + "  <isolated>false</isolated>"
                        + "</namespace>";
        response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/namespaces/isolated_prefix",
                        xmlPut,
                        "text/xml");
        assertEquals(200, response.getStatus());
        // check that the namespace was correctly updated
        namespace = getCatalog().getNamespaceByPrefix("isolated_prefix");
        assertThat(namespace, notNullValue());
        assertThat(namespace.isIsolated(), is(false));
        // check that the workspace was correctly updated
        workspace = getCatalog().getWorkspaceByName("isolated_prefix");
        assertThat(workspace, notNullValue());
        assertThat(workspace.isIsolated(), is(false));
    }
}
