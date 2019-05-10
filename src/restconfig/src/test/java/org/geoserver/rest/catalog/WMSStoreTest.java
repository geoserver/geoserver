/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
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

public class WMSStoreTest extends CatalogRESTTestSupport {
    @ClassRule public static TestHttpClientRule clientMocker = new TestHttpClientRule();
    private static String capabilities;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // we need to add a wms store
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMSStoreInfo wms = cb.buildWMSStore("demo");
        wms.setCapabilitiesURL(capabilities);
        catalog.add(wms);
        cb.setStore(wms);
        WMSLayerInfo layer = cb.buildWMSLayer("world4326");
        catalog.add(layer);
    }

    @BeforeClass
    public static void mockServer() throws Exception {
        capabilities =
                clientMocker.getServer()
                        + "/geoserver/wms?REQUEST=GetCapabilities&VERSION=1.3.0&SERVICE=WMS";
        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL(capabilities),
                new MockHttpResponse(WMSStoreTest.class.getResource("caps130.xml"), "text/xml"));
        clientMocker.bind(client, capabilities);
    }

    @Test
    public void testBeanPresent() throws Exception {
        assertThat(
                GeoServerExtensions.extensions(RestBaseController.class),
                hasItem(instanceOf(WMSStoreController.class)));
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores.xml");
        assertEquals("wmsStores", dom.getDocumentElement().getNodeName());
        assertEquals(
                catalog.getStoresByWorkspace("sf", WMSStoreInfo.class).size(),
                dom.getElementsByTagName("wmsStore").getLength());
    }

    @Test
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores.json");
        assertTrue(json instanceof JSONObject);

        Object stores = ((JSONObject) json).getJSONObject("wmsStores").get("wmsStore");
        assertNotNull(stores);

        if (stores instanceof JSONArray) {
            assertEquals(
                    catalog.getStoresByWorkspace("sf", WMSStoreInfo.class).size(),
                    ((JSONArray) stores).size());
        } else {
            assertEquals(1, catalog.getStoresByWorkspace("sf", WMSStoreInfo.class).size());
        }
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores.html");
        List<WMSStoreInfo> stores = catalog.getStoresByWorkspace("sf", WMSStoreInfo.class);

        NodeList links = xp.getMatchingNodes("//html:a", dom);
        assertEquals(stores.size(), links.getLength());

        for (int i = 0; i < stores.size(); i++) {
            WMSStoreInfo store = stores.get(i);
            Element link = (Element) links.item(i);

            assertTrue(link.getAttribute("href").endsWith(store.getName() + ".html"));
        }
    }

    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals(
                405,
                putAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores")
                        .getStatus());
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores")
                        .getStatus());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo.xml");
        assertEquals("wmsStore", dom.getDocumentElement().getNodeName());
        assertEquals("demo", xp.evaluate("/wmsStore/name", dom));
        assertEquals("sf", xp.evaluate("/wmsStore/workspace/name", dom));
        assertXpathExists("/wmsStore/capabilitiesURL", dom);
    }

    @Test
    public void testGetAsHTML() throws Exception {
        WMSStoreInfo store = catalog.getStoreByName("sf", "demo", WMSStoreInfo.class);
        assertThat(store, notNullValue());
        List<WMSLayerInfo> resources = catalog.getResourcesByStore(store, WMSLayerInfo.class);
        assertThat(resources, not(empty()));

        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo.html");

        WMSStoreInfo wms = catalog.getStoreByName("demo", WMSStoreInfo.class);

        List<WMSLayerInfo> wmsLayers = catalog.getResourcesByStore(wms, WMSLayerInfo.class);

        NodeList links = xp.getMatchingNodes("//html:a", dom);
        assertEquals(wmsLayers.size(), links.getLength());

        for (int i = 0; i < wmsLayers.size(); i++) {
            WMSLayerInfo wl = wmsLayers.get(i);
            Element link = (Element) links.item(i);

            assertTrue(link.getAttribute("href").endsWith(wl.getName() + ".html"));
        }
    }

    @Test
    public void testGetWrongWMSStore() throws Exception {
        // Parameters for the request
        String ws = "sf";
        String wms = "sfssssss";
        // Request path
        String requestPath =
                RestBaseController.ROOT_PATH + "/workspaces/" + ws + "/wmsstores/" + wms + ".html";
        // Exception path
        String exception = "No such wms store: " + ws + "," + wms;
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
                "<wmsStore>"
                        + "<name>newWMSStore</name>"
                        + "<capabilitiesURL>http://somehost/wms?</capabilitiesURL>"
                        + "<workspace>sf</workspace>"
                        + "</wmsStore>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores", xml, "text/xml");

        assertThat(response, hasStatus(HttpStatus.CREATED));
        assertThat(
                response, hasHeader("Location", endsWith("/workspaces/sf/wmsstores/newWMSStore")));

        WMSStoreInfo newStore = catalog.getStoreByName("newWMSStore", WMSStoreInfo.class);
        assertNotNull(newStore);

        assertEquals("http://somehost/wms?", newStore.getCapabilitiesURL());
    }

    @Test
    public void testPostAsXMLNoWorkspace() throws Exception {

        String xml =
                "<wmsStore>"
                        + "<name>newWMSStore</name>"
                        + "<capabilitiesURL>http://somehost/wms?</capabilitiesURL>"
                        + "</wmsStore>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores", xml, "text/xml");

        assertThat(response, hasStatus(HttpStatus.CREATED));
        assertThat(
                response, hasHeader("Location", endsWith("/workspaces/sf/wmsstores/newWMSStore")));

        WMSStoreInfo newStore = catalog.getStoreByName("newWMSStore", WMSStoreInfo.class);
        assertNotNull(newStore);

        assertEquals("http://somehost/wms?", newStore.getCapabilitiesURL());
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo.json");

        JSONObject store = ((JSONObject) json).getJSONObject("wmsStore");
        assertNotNull(store);

        assertEquals("demo", store.get("name"));
        assertEquals("sf", store.getJSONObject("workspace").get("name"));
        assertEquals(capabilities, store.getString("capabilitiesURL"));
    }

    @Test
    public void testPostAsJSON() throws Exception {
        removeStore("sf", "newWMSStore");
        String json =
                "{'wmsStore':{"
                        + "'capabilitiesURL': 'http://somehost/wms?',"
                        + "'workspace':'sf',"
                        + "'name':'newWMSStore',"
                        + "}"
                        + "}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores",
                        json,
                        "text/json");

        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/sf/wmsstores/newWMSStore"));

        WMSStoreInfo newStore = catalog.getStoreByName("newWMSStore", WMSStoreInfo.class);
        assertNotNull(newStore);

        assertEquals("http://somehost/wms?", newStore.getCapabilitiesURL());
    }

    @Test
    public void testPostToResource() throws Exception {
        String xml =
                "<wmsStore>" + "<name>demo</name>" + "<enabled>false</enabled>" + "</wmsStore>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo",
                        xml,
                        "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testPut() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo.xml");
        assertXpathEvaluatesTo("true", "/wmsStore/enabled", dom);

        String xml =
                "<wmsStore>" + "<name>demo</name>" + "<enabled>false</enabled>" + "</wmsStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo.xml");
        assertXpathEvaluatesTo("false", "/wmsStore/enabled", dom);

        assertFalse(catalog.getStoreByName("sf", "demo", WMSStoreInfo.class).isEnabled());
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        WMSStoreInfo wsi = catalog.getStoreByName("sf", "demo", WMSStoreInfo.class);
        wsi.setEnabled(true);
        catalog.save(wsi);
        assertTrue(wsi.isEnabled());
        int maxConnections = wsi.getMaxConnections();
        int readTimeout = wsi.getReadTimeout();
        int connectTimeout = wsi.getConnectTimeout();
        boolean useConnectionPooling = wsi.isUseConnectionPooling();

        String xml = "<wmsStore>" + "<name>demo</name>" + "</wmsStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        wsi = catalog.getStoreByName("sf", "demo", WMSStoreInfo.class);

        assertTrue(wsi.isEnabled());
        assertEquals(maxConnections, wsi.getMaxConnections());
        assertEquals(readTimeout, wsi.getReadTimeout());
        assertEquals(connectTimeout, wsi.getConnectTimeout());
        assertEquals(useConnectionPooling, wsi.isUseConnectionPooling());
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = "<wmsStore>" + "<name>changed</name>" + "</wmsStore>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/nonExistant",
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
        removeStore("sf", "newWMSStore");
        testPostAsXML();
        assertNotNull(catalog.getStoreByName("sf", "newWMSStore", WMSStoreInfo.class));

        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmsstores/newWMSStore")
                        .getStatus());
        assertNull(catalog.getStoreByName("sf", "newWMSStore", WMSStoreInfo.class));
    }

    //    public void testDeleteNonEmptyForbidden() throws Exception {
    //        assertEquals( 403,
    // deleteAsServletResponse(RestBaseController.ROOT_PATH+"/workspaces/sf/datastores/sf").getStatusCode());
    //    }

    @Test
    public void testPutNameChangeForbidden() throws Exception {
        String xml = "<wmsStore>" + "<name>newName</name>" + "</wmsStore>";
        assertEquals(
                403,
                putAsServletResponse(
                                RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo",
                                xml,
                                "text/xml")
                        .getStatus());
    }

    @Test
    public void testPutWorkspaceChangeForbidden() throws Exception {
        String xml = "<wmsStore>" + "<workspace>gs</workspace>" + "</wmsStore>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo",
                        xml,
                        "text/xml");
        assertThat(response, hasStatus(HttpStatus.FORBIDDEN));
    }
}
