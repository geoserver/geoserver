/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.geoserver.rest.catalog.HttpTestUtils.hasHeader;
import static org.geoserver.rest.catalog.HttpTestUtils.hasStatus;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

import java.net.URL;
import java.util.List;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.TestHttpClientRule;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WMTSStoreTest extends CatalogRESTTestSupport {

    private static final String LAYER_NAME = "AMSR2_Snow_Water_Equivalent";

    @ClassRule public static TestHttpClientRule clientMocker = new TestHttpClientRule();
    private static String capabilities;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // we need to add a wmts store
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMTSStoreInfo wmts = cb.buildWMTSStore("demo");
        wmts.setCapabilitiesURL(capabilities);
        catalog.add(wmts);
        cb.setStore(wmts);
        WMTSLayerInfo layer = cb.buildWMTSLayer(LAYER_NAME);
        catalog.add(layer);
    }

    @BeforeClass
    public static void mockServer() throws Exception {
        capabilities =
                clientMocker.getServer()
                        + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS";
        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL(capabilities),
                new MockHttpResponse(
                        WMTSStoreTest.class.getResource("nasa.getcapa.xml"), "text/xml"));
        clientMocker.bind(client, capabilities);
    }

    @Test
    public void testBeanPresent() throws Exception {
        assertThat(
                GeoServerExtensions.extensions(RestBaseController.class),
                hasItem(instanceOf(WMTSStoreController.class)));
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores.xml");
        assertEquals("wmtsStores", dom.getDocumentElement().getNodeName());
        assertEquals(
                catalog.getStoresByWorkspace("sf", WMTSStoreInfo.class).size(),
                dom.getElementsByTagName("wmtsStore").getLength());
    }

    @Test
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores.json");
        assertTrue(json instanceof JSONObject);

        Object stores = ((JSONObject) json).getJSONObject("wmtsStores").get("wmtsStore");
        assertNotNull(stores);

        if (stores instanceof JSONArray) {
            assertEquals(
                    catalog.getStoresByWorkspace("sf", WMTSStoreInfo.class).size(),
                    ((JSONArray) stores).size());
        } else {
            assertEquals(1, catalog.getStoresByWorkspace("sf", WMTSStoreInfo.class).size());
        }
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores.html");
        List<WMTSStoreInfo> stores = catalog.getStoresByWorkspace("sf", WMTSStoreInfo.class);

        NodeList links = xp.getMatchingNodes("//html:a", dom);
        assertEquals(stores.size(), links.getLength());

        for (int i = 0; i < stores.size(); i++) {
            WMTSStoreInfo store = stores.get(i);
            Element link = (Element) links.item(i);

            assertTrue(link.getAttribute("href").endsWith(store.getName() + ".html"));
        }
    }

    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals(
                405,
                putAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores")
                        .getStatus());
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores")
                        .getStatus());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo.xml");
        assertEquals("wmtsStore", dom.getDocumentElement().getNodeName());
        assertEquals("demo", xp.evaluate("/wmtsStore/name", dom));
        assertEquals("sf", xp.evaluate("/wmtsStore/workspace/name", dom));
        assertXpathExists("/wmtsStore/capabilitiesURL", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        WMTSStoreInfo store = catalog.getStoreByName("sf", "demo", WMTSStoreInfo.class);
        assertThat(store, notNullValue());
        List<WMTSLayerInfo> resources = catalog.getResourcesByStore(store, WMTSLayerInfo.class);
        assertThat(resources, not(empty()));

        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo.html");

        WMTSStoreInfo wmts = catalog.getStoreByName("demo", WMTSStoreInfo.class);

        List<WMTSLayerInfo> wmtsLayers = catalog.getResourcesByStore(wmts, WMTSLayerInfo.class);

        NodeList links = xp.getMatchingNodes("//html:a", dom);
        assertEquals(wmtsLayers.size(), links.getLength());

        for (int i = 0; i < wmtsLayers.size(); i++) {
            WMTSLayerInfo wl = wmtsLayers.get(i);
            Element link = (Element) links.item(i);

            assertTrue(link.getAttribute("href").endsWith(wl.getName() + ".html"));
        }
    }

    @Test
    public void testGetWrongWMTSStore() throws Exception {
        // Parameters for the request
        String ws = "sf";
        String wmts = "sfssssss";
        // Request path
        String requestPath =
                RestBaseController.ROOT_PATH
                        + "/workspaces/"
                        + ws
                        + "/wmtsstores/"
                        + wmts
                        + ".html";
        // Exception path
        String exception = "No such wmts store: " + ws + "," + wmts;
        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(exception));
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(exception));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    public void testPostAsXML() throws Exception {

        String xml =
                "<wmtsStore>"
                        + "<name>newWMTSStore</name>"
                        + "<capabilitiesURL>http://somehost/wmts?</capabilitiesURL>"
                        + "<workspace>sf</workspace>"
                        + "</wmtsStore>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores",
                        xml,
                        "text/xml");

        assertThat(response, hasStatus(HttpStatus.CREATED));
        assertThat(
                response,
                hasHeader("Location", endsWith("/workspaces/sf/wmtsstores/newWMTSStore")));

        WMTSStoreInfo newStore = catalog.getStoreByName("newWMTSStore", WMTSStoreInfo.class);
        assertNotNull(newStore);

        assertEquals("http://somehost/wmts?", newStore.getCapabilitiesURL());
    }

    @Test
    public void testPostAsXMLNoWorkspace() throws Exception {

        String xml =
                "<wmtsStore>"
                        + "<name>newWMTSStore</name>"
                        + "<capabilitiesURL>http://somehost/wmts?</capabilitiesURL>"
                        + "</wmtsStore>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores",
                        xml,
                        "text/xml");

        assertThat(response, hasStatus(HttpStatus.CREATED));
        assertThat(
                response,
                hasHeader("Location", endsWith("/workspaces/sf/wmtsstores/newWMTSStore")));

        WMTSStoreInfo newStore = catalog.getStoreByName("newWMTSStore", WMTSStoreInfo.class);
        assertNotNull(newStore);

        assertEquals("http://somehost/wmts?", newStore.getCapabilitiesURL());
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo.json");

        JSONObject store = ((JSONObject) json).getJSONObject("wmtsStore");
        assertNotNull(store);

        assertEquals("demo", store.get("name"));
        assertEquals("sf", store.getJSONObject("workspace").get("name"));
        assertEquals(capabilities, store.getString("capabilitiesURL"));
    }

    @Test
    public void testPostAsJSON() throws Exception {
        removeStore("sf", "newWMTSStore");
        String json =
                "{'wmtsStore':{"
                        + "'capabilitiesURL': 'http://somehost/wmts?',"
                        + "'workspace':'sf',"
                        + "'name':'newWMTSStore',"
                        + "}"
                        + "}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores",
                        json,
                        "text/json");

        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(
                response.getHeader("Location").endsWith("/workspaces/sf/wmtsstores/newWMTSStore"));

        WMTSStoreInfo newStore = catalog.getStoreByName("newWMTSStore", WMTSStoreInfo.class);
        assertNotNull(newStore);

        assertEquals("http://somehost/wmts?", newStore.getCapabilitiesURL());
    }

    @Test
    public void testPostToResource() throws Exception {
        String xml =
                "<wmtsStore>" + "<name>demo</name>" + "<enabled>false</enabled>" + "</wmtsStore>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo",
                        xml,
                        "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testPut() throws Exception {
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo.xml");
        assertXpathEvaluatesTo("true", "/wmtsStore/enabled", dom);

        String xml =
                "<wmtsStore>" + "<name>demo</name>" + "<enabled>false</enabled>" + "</wmtsStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo.xml");
        assertXpathEvaluatesTo("false", "/wmtsStore/enabled", dom);

        assertFalse(catalog.getStoreByName("sf", "demo", WMTSStoreInfo.class).isEnabled());
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        WMTSStoreInfo wsi = catalog.getStoreByName("sf", "demo", WMTSStoreInfo.class);
        wsi.setEnabled(true);
        catalog.save(wsi);
        assertTrue(wsi.isEnabled());
        int maxConnections = wsi.getMaxConnections();
        int readTimeout = wsi.getReadTimeout();
        int connectTimeout = wsi.getConnectTimeout();
        boolean useConnectionPooling = wsi.isUseConnectionPooling();

        String xml = "<wmtsStore>" + "<name>demo</name>" + "</wmtsStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        wsi = catalog.getStoreByName("sf", "demo", WMTSStoreInfo.class);

        assertTrue(wsi.isEnabled());
        assertEquals(maxConnections, wsi.getMaxConnections());
        assertEquals(readTimeout, wsi.getReadTimeout());
        assertEquals(connectTimeout, wsi.getConnectTimeout());
        assertEquals(useConnectionPooling, wsi.isUseConnectionPooling());
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = "<wmtsStore>" + "<name>changed</name>" + "</wmtsStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/nonExistant",
                        xml,
                        "text/xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/datastores/nonExistant")
                        .getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        removeStore("sf", "newWMTSStore");
        testPostAsXML();
        assertNotNull(catalog.getStoreByName("sf", "newWMTSStore", WMTSStoreInfo.class));

        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmtsstores/newWMTSStore")
                        .getStatus());
        assertNull(catalog.getStoreByName("sf", "newWMTSStore", WMTSStoreInfo.class));
    }

    @Test
    public void testPutNameChangeForbidden() throws Exception {
        String xml = "<wmtsStore>" + "<name>newName</name>" + "</wmtsStore>";
        assertEquals(
                403,
                putAsServletResponse(
                                RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo",
                                xml,
                                "text/xml")
                        .getStatus());
    }

    @Test
    public void testPutWorkspaceChangeForbidden() throws Exception {
        String xml = "<wmtsStore>" + "<workspace>gs</workspace>" + "</wmtsStore>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo",
                        xml,
                        "text/xml");
        assertThat(response, hasStatus(HttpStatus.FORBIDDEN));
    }
}
