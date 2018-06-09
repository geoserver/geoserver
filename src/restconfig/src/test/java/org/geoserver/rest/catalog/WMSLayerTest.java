/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.geoserver.rest.catalog.HttpTestUtils.hasHeader;
import static org.geoserver.rest.catalog.HttpTestUtils.hasStatus;
import static org.geoserver.rest.catalog.HttpTestUtils.istream;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

import java.net.URL;
import junit.framework.AssertionFailedError;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.TestHttpClientRule;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class WMSLayerTest extends CatalogRESTTestSupport {
    @Rule public TestHttpClientRule clientMocker = new TestHttpClientRule();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // we need to add a wms store
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMSStoreInfo wms = cb.buildWMSStore("demo");
        wms.setCapabilitiesURL(
                clientMocker.getServer()
                        + "/geoserver/wms?REQUEST=GetCapabilities&VERSION=1.3.0&SERVICE=WMS");
        catalog.add(wms);

        // and a wms layer as well (cannot use the builder, would turn this test into an online one
        addStatesWmsLayer();
    }

    @Before
    public void addStatesWmsLayer() throws Exception {
        String capabilities =
                clientMocker.getServer()
                        + "/geoserver/wms?REQUEST=GetCapabilities&VERSION=1.3.0&SERVICE=WMS";
        WMSLayerInfo wml = catalog.getResourceByName("sf", "states", WMSLayerInfo.class);
        if (wml == null) {
            wml = catalog.getFactory().createWMSLayer();
            wml.setName("states");
            wml.setNativeName("topp:states");
            wml.setStore(catalog.getStoreByName("demo", WMSStoreInfo.class));
            wml.setCatalog(catalog);
            wml.setNamespace(catalog.getNamespaceByPrefix("sf"));
            wml.setSRS("EPSG:4326");
            CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
            wml.setNativeCRS(wgs84);
            wml.setLatLonBoundingBox(new ReferencedEnvelope(-110, 0, -60, 50, wgs84));
            wml.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

            catalog.add(wml);
        }

        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL(capabilities),
                new MockHttpResponse(getClass().getResource("caps130.xml"), "text/xml"));
        clientMocker.bind(client, capabilities);
    }

    @After
    public void removeLayer() throws Exception {
        LayerInfo l = catalog.getLayerByName(new NameImpl("sf", "states"));
        if (l != null) {
            catalog.remove(l);
        }
    }

    @Before
    public void removeBugsites() throws Exception {
        LayerInfo l = catalog.getLayerByName(new NameImpl("sf", "bugsites"));
        if (l != null) {
            catalog.remove(l);
        }

        ResourceInfo r = catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class);
        if (r != null) {
            catalog.remove(r);
        }
    }

    @Test
    public void testBeanPresent() throws Exception {
        assertThat(
                GeoServerExtensions.extensions(RestBaseController.class),
                hasItem(instanceOf(WMSLayerController.class)));
    }

    @Test
    public void testGetAllByWorkspace() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/sf/wmslayers.xml");
        assertThat(response, hasStatus(HttpStatus.OK));
        Document dom = dom(istream(response));
        assertEquals(
                catalog.getResourcesByNamespace(
                                catalog.getNamespaceByPrefix("sf"), WMSLayerInfo.class)
                        .size(),
                dom.getElementsByTagName("wmsLayer").getLength());
    }

    @Test
    public void testGetAllByWMSStore() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmsstores/demo/wmslayers.xml");
        assertThat(response, hasStatus(HttpStatus.OK));
        Document dom = dom(istream(response));

        assertEquals(1, dom.getElementsByTagName("wmsLayer").getLength());
        assertXpathEvaluatesTo("1", "count(//wmsLayer/name[text()='states'])", dom);
    }

    @Test
    public void testGetAllAvailable() throws Exception {

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmsstores/demo/wmslayers.xml?list=available",
                        200);
        // print(dom)

        // can't control the demo server enough to check the type names, but it should have
        // something
        // more than just topp:states
        assertXpathExists("/list/wmsLayerName[text() = 'world4326']", dom);
        assertXpathExists("/list/wmsLayerName[text() = 'anotherLayer']", dom);
        assertXpathNotExists("/list/wmsLayerName[text() = 'topp:states']", dom);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetAllAvailableJSON() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmsstores/demo/wmslayers.json?list=available");
        assertThat(response, hasStatus(HttpStatus.OK));

        JSON json = json(response);
        JSONArray names = (JSONArray) ((JSONObject) ((JSONObject) json).get("list")).get("string");
        assertThat(
                names, (Matcher) containsInAnyOrder(equalTo("world4326"), equalTo("anotherLayer")));
    }

    @Override
    protected JSON getAsJSON(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        try {
            return json(response);
        } catch (JSONException ex) {
            throw new AssertionFailedError(
                    "Invalid JSON: \"" + response.getContentAsString() + "\"");
        }
    }

    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals(
                405,
                putAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmsstores/demo/wmslayers")
                        .getStatus());
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmsstores/demo/wmslayers")
                        .getStatus());
    }

    @Test
    public void testPostAsXML() throws Exception {

        assertThat(catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class), nullValue());

        String xml =
                "<wmsLayer>"
                        + "<name>bugsites</name>"
                        + "<nativeName>world4326</nativeName>"
                        + "<srs>EPSG:4326</srs>"
                        + "<nativeCRS>EPSG:4326</nativeCRS>"
                        + "<store>demo</store>"
                        + "</wmsLayer>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo/wmslayers/",
                        xml,
                        "text/xml");

        assertThat(response, hasStatus(HttpStatus.CREATED));

        assertThat(
                response,
                hasHeader(
                        "Location",
                        Matchers.endsWith("/workspaces/sf/wmsstores/demo/wmslayers/bugsites")));

        WMSLayerInfo layer = catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class);
        assertThat(layer, hasProperty("nativeBoundingBox", notNullValue()));
    }

    @Test
    public void testPostAsXMLNoWorkspace() throws Exception {

        assertThat(catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class), nullValue());

        String xml =
                "<wmsLayer>"
                        + "<name>bugsites</name>"
                        + "<nativeName>world4326</nativeName>"
                        + "<srs>EPSG:4326</srs>"
                        + "<nativeCRS>EPSG:4326</nativeCRS>"
                        + "<store>demo</store>"
                        + "</wmsLayer>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmslayers/",
                        xml,
                        "text/xml");

        assertThat(response, hasStatus(HttpStatus.CREATED));

        assertThat(
                response,
                hasHeader("Location", Matchers.endsWith("/workspaces/sf/wmslayers/bugsites")));

        WMSLayerInfo layer = catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class);
        assertThat(layer, hasProperty("nativeBoundingBox", notNullValue()));
    }

    @Test
    public void testPostAsJSON() throws Exception {

        assertThat(catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class), nullValue());

        String json =
                "{"
                        + "'wmsLayer':{"
                        + "'name':'bugsites',"
                        + "'nativeName':'world4326',"
                        + "'srs':'EPSG:4326',"
                        + "'nativeCRS':'EPSG:4326',"
                        + "'store':'demo'"
                        + "}"
                        + "}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmsstores/demo/wmslayers/",
                        json,
                        "text/json");

        assertThat(response, hasStatus(HttpStatus.CREATED));

        assertThat(
                response,
                hasHeader(
                        "Location",
                        Matchers.endsWith("/workspaces/sf/wmsstores/demo/wmslayers/bugsites")));

        WMSLayerInfo layer = catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class);
        assertThat(layer, hasProperty("nativeBoundingBox", notNullValue()));
    }

    @Test
    public void testPostToResource() throws Exception {
        String xml = "<wmsLayer>" + "<name>og:restricted</name>" + "</wmsLayer>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmsstores/demo/wmslayers/states",
                        xml,
                        "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testGetAsXML() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmslayers/states.xml");
        assertThat(response, hasStatus(HttpStatus.OK));
        Document dom = dom(istream(response));

        assertEquals("wmsLayer", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("states", "/wmsLayer/name", dom);
        assertXpathEvaluatesTo("EPSG:4326", "/wmsLayer/srs", dom);
        assertEquals(CRS.decode("EPSG:4326").toWKT(), xp.evaluate("/wmsLayer/nativeCRS", dom));

        WMSLayerInfo wml = catalog.getResourceByName("sf", "states", WMSLayerInfo.class);

        ReferencedEnvelope re = wml.getLatLonBoundingBox();
        assertXpathEvaluatesTo(re.getMinX() + "", "/wmsLayer/latLonBoundingBox/minx", dom);
        assertXpathEvaluatesTo(re.getMaxX() + "", "/wmsLayer/latLonBoundingBox/maxx", dom);
        assertXpathEvaluatesTo(re.getMinY() + "", "/wmsLayer/latLonBoundingBox/miny", dom);
        assertXpathEvaluatesTo(re.getMaxY() + "", "/wmsLayer/latLonBoundingBox/maxy", dom);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json =
                getAsJSON(RestBaseController.ROOT_PATH + "/workspaces/sf/wmslayers/states.json");
        JSONObject featureType = ((JSONObject) json).getJSONObject("wmsLayer");
        assertNotNull(featureType);

        assertEquals("states", featureType.get("name"));
        assertEquals(CRS.decode("EPSG:4326").toWKT(), featureType.get("nativeCRS"));
        assertEquals("EPSG:4326", featureType.get("srs"));
    }

    @Test
    public void testGetAsHTML() throws Exception {
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/sf/wmslayers/states.html");
        // print(dom);
    }

    @Test
    public void testGetWrongWMSLayer() throws Exception {
        // Parameters for the request
        String ws = "sf";
        String wms = "demo";
        String wl = "statessssss";
        // Request path
        String requestPath =
                RestBaseController.ROOT_PATH + "/workspaces/" + ws + "/wmslayers/" + wl + ".html";
        String requestPath2 =
                RestBaseController.ROOT_PATH
                        + "/workspaces/"
                        + ws
                        + "/wmsstores/"
                        + wms
                        + "/wmslayers/"
                        + wl
                        + ".html";
        // Exception path
        String exception = "No such cascaded wms: " + ws + "," + wl;
        String exception2 = "No such cascaded wms layer: " + ws + "," + wms + "," + wl;

        // CASE 1: No wmsstore set

        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertThat(response, hasStatus(HttpStatus.NOT_FOUND));
        assertThat(response.getContentAsString(), containsString(exception));

        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertThat(response, hasStatus(HttpStatus.NOT_FOUND));
        assertThat(response.getContentAsString(), not(containsString(exception)));

        // No exception thrown
        assertThat(response.getContentAsString(), isEmptyString());

        // CASE 2: wmsstore set

        // First request should thrown an exception
        response = getAsServletResponse(requestPath2);
        assertThat(response, hasStatus(HttpStatus.NOT_FOUND));
        assertThat(response.getContentAsString(), containsString(exception));

        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath2 + "?quietOnNotFound=true");
        assertThat(response, hasStatus(HttpStatus.NOT_FOUND));
        assertThat(response.getContentAsString(), not(containsString(exception)));

        // No exception thrown
        assertThat(response.getContentAsString(), isEmptyString());
    }

    @Test
    public void testPut() throws Exception {
        String xml = "<wmsLayer>" + "<title>Lots of states here</title>" + "</wmsLayer>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmsstores/demo/wmslayers/states",
                        xml,
                        "text/xml");
        assertThat(response, hasStatus(HttpStatus.OK));

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmsstores/demo/wmslayers/states.xml");
        assertXpathEvaluatesTo("Lots of states here", "/wmsLayer/title", dom);

        WMSLayerInfo wli = catalog.getResourceByName("sf", "states", WMSLayerInfo.class);
        assertEquals("Lots of states here", wli.getTitle());
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        WMSLayerInfo wli = catalog.getResourceByName("sf", "states", WMSLayerInfo.class);
        wli.setEnabled(true);
        catalog.save(wli);
        wli = catalog.getResourceByName("sf", "states", WMSLayerInfo.class);
        assertTrue(wli.isEnabled());
        boolean isAdvertised = wli.isAdvertised();

        String xml = "<wmsLayer>" + "<title>Lots of states here</title>" + "</wmsLayer>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmsstores/demo/wmslayers/states",
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        wli = catalog.getResourceByName("sf", "states", WMSLayerInfo.class);
        assertTrue(wli.isEnabled());
        assertEquals(isAdvertised, wli.isAdvertised());
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = "<wmsLayer>" + "<title>new title</title>" + "</wmsLayer>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmsstores/demo/wmslayers/bugsites",
                        xml,
                        "text/xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        assertNotNull(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
        assertThat(
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmsstores/demo/wmslayers/states"),
                hasStatus(HttpStatus.OK));
        assertNull(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmsstores/demo/wmslayers/NonExistent")
                        .getStatus());
    }

    void addLayer() {
        LayerInfo l = catalog.getLayerByName(new NameImpl("sf", "states"));
        if (l == null) {
            l = catalog.getFactory().createLayer();
            l.setResource(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
            catalog.add(l);
        }
    }

    @Test
    public void testDeleteNonRecursive() throws Exception {
        addLayer();

        assertNotNull(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
        assertEquals(
                403,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmsstores/demo/wmslayers/states")
                        .getStatus());
    }

    @Test
    public void testDeleteRecursive() throws Exception {
        addLayer();

        assertNotNull(catalog.getLayerByName("sf:states"));
        assertNotNull(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));

        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmsstores/demo/wmslayers/states?recurse=true")
                        .getStatus());

        assertNull(catalog.getLayerByName("sf:states"));
        assertNull(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
    }

    @Test
    public void testResourceLink() throws Exception {
        addLayer();

        Document doc = getAsDOM(RestBaseController.ROOT_PATH + "/layers/states.xml");

        XpathEngine xpath = XMLUnit.newXpathEngine();
        String resourceUrl = xpath.evaluate("//resource/atom:link/@href", doc);
        resourceUrl = resourceUrl.substring(resourceUrl.indexOf("/rest"));

        doc = getAsDOM(resourceUrl);
        assertXpathEvaluatesTo("states", "/wmsLayer/name", doc);
    }
}
