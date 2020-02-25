/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
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
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.TestHttpClientRule;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
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

public class WMTSLayerTest extends CatalogRESTTestSupport {

    private static final String LAYER_NAME = "AMSR2_Snow_Water_Equivalent";
    private static final String ANOTHER_LAYER_NAME = "AMSR2_Soil_Moisture_NPD_Day";
    private static final String ANOTHER_LOCAL_NAME = "this_is_the_local_name";

    @Rule public TestHttpClientRule clientMocker = new TestHttpClientRule();

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // we need to add a wmts store
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMTSStoreInfo wmts = cb.buildWMTSStore("demo");
        wmts.setCapabilitiesURL(
                clientMocker.getServer()
                        + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS");
        catalog.add(wmts);

        // and a wmts layer as well (cannot use the builder, would turn this test into an online one
        addWmtsLayer();
    }

    @Before
    public void addWmtsLayer() throws Exception {
        String capabilities =
                clientMocker.getServer()
                        + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS";
        WMTSLayerInfo wml = catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class);
        if (wml == null) {
            wml = catalog.getFactory().createWMTSLayer();
            wml.setName(LAYER_NAME);
            wml.setNativeName("topp:" + LAYER_NAME);
            wml.setStore(catalog.getStoreByName("demo", WMTSStoreInfo.class));
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
                new MockHttpResponse(getClass().getResource("nasa.getcapa.xml"), "text/xml"));
        clientMocker.bind(client, capabilities);
    }

    @After
    public void removeLayer() throws Exception {
        LayerInfo l = catalog.getLayerByName(new NameImpl("sf", LAYER_NAME));
        if (l != null) {
            catalog.remove(l);
        }
    }

    @Before
    public void removeLocalLayer() throws Exception {
        LayerInfo l = catalog.getLayerByName(new NameImpl("sf", ANOTHER_LOCAL_NAME));
        if (l != null) {
            catalog.remove(l);
        }

        ResourceInfo r = catalog.getResourceByName("sf", ANOTHER_LOCAL_NAME, WMTSLayerInfo.class);
        if (r != null) {
            catalog.remove(r);
        }
    }

    @Test
    public void testBeanPresent() throws Exception {
        assertThat(
                GeoServerExtensions.extensions(RestBaseController.class),
                hasItem(instanceOf(WMTSLayerController.class)));
    }

    @Test
    public void testGetAllByWorkspace() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtslayers.xml");
        assertThat(response, hasStatus(HttpStatus.OK));
        Document dom = dom(istream(response));
        assertEquals(
                catalog.getResourcesByNamespace(
                                catalog.getNamespaceByPrefix("sf"), WMTSLayerInfo.class)
                        .size(),
                dom.getElementsByTagName("wmtsLayer").getLength());
    }

    @Test
    public void testGetAllByWMTSStore() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo/layers.xml");
        assertThat(response, hasStatus(HttpStatus.OK));
        Document dom = dom(istream(response));

        assertEquals(1, dom.getElementsByTagName("wmtsLayer").getLength());
        assertXpathEvaluatesTo("1", "count(//wmtsLayer/name[text()='" + LAYER_NAME + "'])", dom);
    }

    @Test
    public void testGetAllAvailable() throws Exception {

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtsstores/demo/layers.xml?list=available",
                        200);
        print(dom);

        // can't control the demo server enough to check the type names, but it should have
        // something
        // more than just topp:states
        assertXpathExists("/list/wmtsLayerName[text() = '" + LAYER_NAME + "']", dom);
        assertXpathExists("/list/wmtsLayerName[text() = '" + ANOTHER_LAYER_NAME + "']", dom);
        assertXpathNotExists("/list/wmtsLayerName[text() = 'topp:" + LAYER_NAME + "']", dom);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetAllAvailableJSON() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtsstores/demo/layers.json?list=available");
        assertThat(response, hasStatus(HttpStatus.OK));

        JSON json = json(response);
        JSONArray names = (JSONArray) ((JSONObject) ((JSONObject) json).get("list")).get("string");
        assertThat(names, (Matcher) hasItems(equalTo(LAYER_NAME), equalTo(ANOTHER_LAYER_NAME)));
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
                                        + "/workspaces/sf/wmtsstores/demo/layers")
                        .getStatus());
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals(
                405,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmtsstores/demo/layers")
                        .getStatus());
    }

    @Test
    public void testPostAsXML() throws Exception {

        assertThat(
                catalog.getResourceByName("sf", ANOTHER_LOCAL_NAME, WMTSLayerInfo.class),
                nullValue());

        String xml =
                "<wmtsLayer>"
                        + "<name>"
                        + ANOTHER_LOCAL_NAME
                        + "</name>"
                        + "<nativeName>"
                        + ANOTHER_LAYER_NAME
                        + "</nativeName>"
                        + "<srs>EPSG:4326</srs>"
                        + "<nativeCRS>EPSG:4326</nativeCRS>"
                        + "<store>demo</store>"
                        + "</wmtsLayer>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo/layers/",
                        xml,
                        "text/xml");

        assertThat(response, hasStatus(HttpStatus.CREATED));

        assertThat(
                response,
                hasHeader(
                        "Location",
                        Matchers.endsWith(
                                "/workspaces/sf/wmtsstores/demo/layers/" + ANOTHER_LOCAL_NAME)));

        WMTSLayerInfo layer =
                catalog.getResourceByName("sf", ANOTHER_LOCAL_NAME, WMTSLayerInfo.class);
        assertThat(layer, hasProperty("nativeBoundingBox", notNullValue()));
    }

    @Test
    public void testPostAsXMLNoWorkspace() throws Exception {

        assertThat(
                catalog.getResourceByName("sf", ANOTHER_LOCAL_NAME, WMTSLayerInfo.class),
                nullValue());

        String xml =
                "<wmtsLayer>"
                        + "<name>"
                        + ANOTHER_LOCAL_NAME
                        + "</name>"
                        + "<nativeName>"
                        + ANOTHER_LAYER_NAME
                        + "</nativeName>"
                        + "<srs>EPSG:4326</srs>"
                        + "<nativeCRS>EPSG:4326</nativeCRS>"
                        + "<store>demo</store>"
                        + "</wmtsLayer>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtslayers/",
                        xml,
                        "text/xml");

        assertThat(response, hasStatus(HttpStatus.CREATED));

        assertThat(
                response,
                hasHeader(
                        "Location",
                        Matchers.endsWith("/workspaces/sf/wmtslayers/" + ANOTHER_LOCAL_NAME)));

        WMTSLayerInfo layer =
                catalog.getResourceByName("sf", ANOTHER_LOCAL_NAME, WMTSLayerInfo.class);
        assertThat(layer, hasProperty("nativeBoundingBox", notNullValue()));
    }

    @Test
    public void testPostAsJSON() throws Exception {

        assertThat(
                catalog.getResourceByName("sf", ANOTHER_LOCAL_NAME, WMTSLayerInfo.class),
                nullValue());

        String json =
                "{"
                        + "'wmtsLayer':{"
                        + "'name':'"
                        + ANOTHER_LOCAL_NAME
                        + "',"
                        + "'nativeName':'"
                        + ANOTHER_LAYER_NAME
                        + "',"
                        + "'srs':'EPSG:4326',"
                        + "'nativeCRS':'EPSG:4326',"
                        + "'store':'demo'"
                        + "}"
                        + "}";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/sf/wmtsstores/demo/layers/",
                        json,
                        "text/json");

        assertThat(response, hasStatus(HttpStatus.CREATED));

        assertThat(
                response,
                hasHeader(
                        "Location",
                        Matchers.endsWith(
                                "/workspaces/sf/wmtsstores/demo/layers/" + ANOTHER_LOCAL_NAME)));

        WMTSLayerInfo layer =
                catalog.getResourceByName("sf", ANOTHER_LOCAL_NAME, WMTSLayerInfo.class);
        assertThat(layer, hasProperty("nativeBoundingBox", notNullValue()));
    }

    @Test
    public void testPostToResource() throws Exception {
        String xml = "<wmtsLayer>" + "<name>og:restricted</name>" + "</wmtsLayer>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtsstores/demo/layers/"
                                + LAYER_NAME,
                        xml,
                        "text/xml");
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testGetAsXML() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtslayers/"
                                + LAYER_NAME
                                + ".xml");
        assertThat(response, hasStatus(HttpStatus.OK));
        Document dom = dom(istream(response));

        assertEquals("wmtsLayer", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo(LAYER_NAME, "/wmtsLayer/name", dom);
        assertXpathEvaluatesTo("EPSG:4326", "/wmtsLayer/srs", dom);
        assertEquals(CRS.decode("EPSG:4326").toWKT(), xp.evaluate("/wmtsLayer/nativeCRS", dom));

        WMTSLayerInfo wml = catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class);

        ReferencedEnvelope re = wml.getLatLonBoundingBox();
        assertXpathEvaluatesTo(re.getMinX() + "", "/wmtsLayer/latLonBoundingBox/minx", dom);
        assertXpathEvaluatesTo(re.getMaxX() + "", "/wmtsLayer/latLonBoundingBox/maxx", dom);
        assertXpathEvaluatesTo(re.getMinY() + "", "/wmtsLayer/latLonBoundingBox/miny", dom);
        assertXpathEvaluatesTo(re.getMaxY() + "", "/wmtsLayer/latLonBoundingBox/maxy", dom);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json =
                getAsJSON(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtslayers/"
                                + LAYER_NAME
                                + ".json");
        JSONObject featureType = ((JSONObject) json).getJSONObject("wmtsLayer");
        assertNotNull(featureType);

        assertEquals(LAYER_NAME, featureType.get("name"));
        assertEquals(CRS.decode("EPSG:4326").toWKT(), featureType.get("nativeCRS"));
        assertEquals("EPSG:4326", featureType.get("srs"));
    }

    @Test
    public void testGetAsHTML() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtslayers/"
                                + LAYER_NAME
                                + ".html");
        assertThat(response, hasStatus(HttpStatus.OK));
    }

    @Test
    public void testGetWrongWMTSLayer() throws Exception {
        // Parameters for the request
        String ws = "sf";
        String wmts = "demo";
        String wl = "statessssss";
        // Request path
        String requestPath =
                RestBaseController.ROOT_PATH + "/workspaces/" + ws + "/wmtslayers/" + wl + ".html";
        String requestPath2 =
                RestBaseController.ROOT_PATH
                        + "/workspaces/"
                        + ws
                        + "/wmtsstores/"
                        + wmts
                        + "/layers/"
                        + wl
                        + ".html";
        // Exception path
        String exception = "No such cascaded wmts: " + ws + "," + wl;
        String exception2 = "No such cascaded wmts layer: " + ws + "," + wmts + "," + wl;

        // CASE 1: No wmtsstore set

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

        // CASE 2: wmtsstore set

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
        String xml = "<wmtsLayer>" + "<title>Lots of states here</title>" + "</wmtsLayer>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtsstores/demo/layers/"
                                + LAYER_NAME,
                        xml,
                        "text/xml");
        assertThat(response, hasStatus(HttpStatus.OK));

        Document dom =
                getAsDOM(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtsstores/demo/layers/"
                                + LAYER_NAME
                                + ".xml");
        assertXpathEvaluatesTo("Lots of states here", "/wmtsLayer/title", dom);

        WMTSLayerInfo wli = catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class);
        assertEquals("Lots of states here", wli.getTitle());
    }

    @Test
    public void testPutNonDestructive() throws Exception {
        WMTSLayerInfo wli = catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class);
        wli.setEnabled(true);
        catalog.save(wli);
        wli = catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class);
        assertTrue(wli.isEnabled());
        boolean isAdvertised = wli.isAdvertised();

        String xml = "<wmtsLayer>" + "<title>Lots of states here</title>" + "</wmtsLayer>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtsstores/demo/layers/"
                                + LAYER_NAME,
                        xml,
                        "text/xml");
        assertEquals(200, response.getStatus());

        wli = catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class);
        assertTrue(wli.isEnabled());
        assertEquals(isAdvertised, wli.isAdvertised());
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = "<wmtsLayer>" + "<title>new title</title>" + "</wmtsLayer>";
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtsstores/demo/layers/bugsites",
                        xml,
                        "text/xml");
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        assertNotNull(catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class));
        assertThat(
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/sf/wmtsstores/demo/layers/"
                                + LAYER_NAME),
                hasStatus(HttpStatus.OK));
        assertNull(catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class));
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals(
                404,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmtsstores/demo/layers/NonExistent")
                        .getStatus());
    }

    void addLayer() {
        LayerInfo l = catalog.getLayerByName(new NameImpl("sf", LAYER_NAME));
        if (l == null) {
            l = catalog.getFactory().createLayer();
            l.setResource(catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class));
            catalog.add(l);
        }
    }

    @Test
    public void testDeleteNonRecursive() throws Exception {
        addLayer();

        assertNotNull(catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class));
        assertEquals(
                403,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmtsstores/demo/layers/"
                                        + LAYER_NAME)
                        .getStatus());
    }

    @Test
    public void testDeleteRecursive() throws Exception {
        addLayer();

        assertNotNull(catalog.getLayerByName("sf:" + LAYER_NAME));
        assertNotNull(catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class));

        assertEquals(
                200,
                deleteAsServletResponse(
                                RestBaseController.ROOT_PATH
                                        + "/workspaces/sf/wmtsstores/demo/layers/"
                                        + LAYER_NAME
                                        + "?recurse=true")
                        .getStatus());

        assertNull(catalog.getLayerByName("sf:" + LAYER_NAME));
        assertNull(catalog.getResourceByName("sf", LAYER_NAME, WMTSLayerInfo.class));
    }
}
