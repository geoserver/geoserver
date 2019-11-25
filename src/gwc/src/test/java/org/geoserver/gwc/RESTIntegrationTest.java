/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.filters.BufferedRequestWrapper;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.StyleParameterFilter;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** Integration test for GeoServer cached layers using the GWC REST API */
public class RESTIntegrationTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
    }

    @Test
    public void testGetLayersList() throws Exception {
        final String url = "gwc/rest/layers.xml";
        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatus());
        assertTrue(sr.getContentType(), sr.getContentType().startsWith("application/xml"));

        Document dom = getAsDOM(url);
        // print(dom);

        ArrayList<String> tileLayerNames = Lists.newArrayList(GWC.get().getTileLayerNames());
        Collections.sort(tileLayerNames);

        XMLUnit.setXpathNamespaceContext(
                new SimpleNamespaceContext(ImmutableMap.of("atom", "http://www.w3.org/2005/Atom")));

        for (String name : tileLayerNames) {
            String xpath = "//layers/layer/name[text() = '" + name + "']";
            assertXpathExists(xpath, dom);

            xpath =
                    "//layers/layer/atom:link[@href = 'http://localhost:8080/geoserver/gwc/rest/layers/"
                            + name
                            + ".xml']";
            assertXpathExists(xpath, dom);
        }
    }

    @Test
    public void testGetLayer() throws Exception {
        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String url = "gwc/rest/layers/" + layerName + ".xml";
        final String id = getCatalog().getLayerByName(layerName).getId();

        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatus());
        assertTrue(sr.getContentType(), sr.getContentType().startsWith("application/xml"));

        Document dom = getAsDOM(url);
        // print(dom);

        assertXpathExists("/GeoServerLayer", dom);
        assertXpathEvaluatesTo(id, "/GeoServerLayer/id", dom);
        assertXpathEvaluatesTo(layerName, "/GeoServerLayer/name", dom);
        assertXpathEvaluatesTo("true", "/GeoServerLayer/enabled", dom);
        assertXpathEvaluatesTo("image/png", "/GeoServerLayer/mimeFormats/string[1]", dom);
        assertXpathEvaluatesTo("image/jpeg", "/GeoServerLayer/mimeFormats/string[2]", dom);
        assertXpathEvaluatesTo(
                "EPSG:900913", "/GeoServerLayer/gridSubsets/gridSubset[1]/gridSetName", dom);
        assertXpathNotExists("/GeoServerLayer/autoCacheStyles", dom);
    }

    /**
     * PUT creates a new layer, shall fail if the layer id is provided and not found in the catalog
     */
    @Test
    public void testPutBadId() throws Exception {
        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String url = "gwc/rest/layers/" + layerName + ".xml";

        MockHttpServletResponse response = putLayer(url, "badId", layerName);

        assertThat(response, hasProperty("status", equalTo(HttpServletResponse.SC_BAD_REQUEST)));
        assertThat(
                response,
                hasProperty(
                        "contentAsString",
                        containsString("No GeoServer Layer or LayerGroup exists with id 'badId'")));
        assertThat(response, hasProperty("contentType", startsWith("text/plain")));
    }

    /**
     * PUT creates a new layer, shall fail if the layer id is not provided, the layer name is, but
     * no such layer is found in the {@link Catalog}
     */
    @Test
    public void testPutNoIdBadLayerName() throws Exception {

        final String url = "gwc/rest/layers/badLayerName.xml";
        MockHttpServletResponse response = putLayer(url, "", "badLayerName");

        assertThat(response, hasProperty("status", equalTo(HttpServletResponse.SC_NOT_FOUND)));
        assertThat(
                response,
                hasProperty(
                        "contentAsString",
                        containsString("GeoServer Layer or LayerGroup 'badLayerName' not found")));
        assertThat(response, hasProperty("contentType", startsWith("text/plain")));
    }

    @Test
    public void testPutGoodIdBadLayerName() throws Exception {

        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String id = getCatalog().getLayerByName(layerName).getId();

        final String url = "gwc/rest/layers/badLayerName.xml";

        MockHttpServletResponse response = putLayer(url, id, "badLayerName");

        String expected =
                "Layer with id '"
                        + id
                        + "' found but name does not match: 'badLayerName'/'"
                        + layerName
                        + "'";
        assertThat(response, hasProperty("status", equalTo(HttpServletResponse.SC_BAD_REQUEST)));
        assertThat(response, hasProperty("contentAsString", containsString(expected)));
        assertThat(response, hasProperty("contentType", startsWith("text/plain")));
    }

    /** Id is optional, layer name mandatory */
    @Test
    public void testPutGoodIdNoLayerName() throws Exception {

        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String id = getCatalog().getLayerByName(layerName).getId();

        final String url = "gwc/rest/layers/" + layerName + ".xml";

        MockHttpServletResponse response = putLayer(url, id, "");

        assertThat(response, hasProperty("status", equalTo(HttpServletResponse.SC_BAD_REQUEST)));
        assertThat(
                response,
                hasProperty("contentAsString", containsString("Layer name not provided")));
        assertThat(response, hasProperty("contentType", startsWith("text/plain")));
    }

    @Test
    public void testPutOverExistingTileLayerSucceeds() throws Exception {

        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String id = getCatalog().getLayerByName(layerName).getId();

        final String url = "gwc/rest/layers/" + layerName + ".xml";

        MockHttpServletResponse response = putLayer(url, id, layerName);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void testPutBadLayerEndpoint() throws Exception {

        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String id = getCatalog().getLayerByName(layerName).getId();

        final String url = "gwc/rest/layers/badEndpoint.xml";

        MockHttpServletResponse response = putLayer(url, id, layerName);

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        // See GWC's TileLayerRestlet
        String expected =
                "There is a mismatch between the name of the  layer in the submission and the URL you specified.";
        assertEquals(
                expected,
                response.getContentAsString()
                        .substring(response.getContentAsString().indexOf(":") + 1));
    }

    @Test
    public void testPutSuccess() throws Exception {

        final String layerName = getLayerId(MockData.FORESTS);
        final String id = getCatalog().getLayerByName(layerName).getId();

        final GWC mediator = GWC.get();
        assertTrue(mediator.tileLayerExists(layerName));
        mediator.removeTileLayers(Lists.newArrayList(layerName));
        assertFalse(mediator.tileLayerExists(layerName));

        final String url = "gwc/rest/layers/" + layerName + ".xml";

        MockHttpServletResponse response = putLayer(url, id, layerName);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        assertTrue(mediator.tileLayerExists(layerName));
    }

    @Test
    public void testPutParameterFilters() throws Exception {
        final String layerName = getLayerId(MockData.LAKES);

        final GWC mediator = GWC.get();
        assertTrue(mediator.tileLayerExists(layerName));
        mediator.removeTileLayers(Lists.newArrayList(layerName));
        assertFalse(mediator.tileLayerExists(layerName));

        final String xml =
                "<GeoServerLayer>" //
                        + " <enabled>true</enabled>" //
                        + " <name>"
                        + layerName
                        + "</name>" //
                        + " <mimeFormats><string>image/png8</string></mimeFormats>" //
                        + " <gridSubsets>" //
                        + "  <gridSubset><gridSetName>GoogleCRS84Quad</gridSetName></gridSubset>" //
                        + "  <gridSubset><gridSetName>EPSG:4326</gridSetName></gridSubset>" //
                        + " </gridSubsets>" //
                        + " <metaWidthHeight><int>9</int><int>6</int></metaWidthHeight>" //
                        + " <parameterFilters>" //
                        + "  <stringParameterFilter>" //
                        + "   <key>BGCOLOR</key>" //
                        + "   <defaultValue>0xFFFFFF</defaultValue>" //
                        + "   <values><string>0x000000</string><string>0x888888</string></values>" //
                        + "  </stringParameterFilter>" //
                        + "  <styleParameterFilter>" //
                        + "   <key>STYLES</key>" //
                        + "   <defaultValue>capital</defaultValue>" //
                        + "   <allowedStyles><string>point</string><string>burg</string></allowedStyles>" //
                        + "  </styleParameterFilter>" //
                        + "  <floatParameterFilter>" //
                        + "   <key>ELEVATION</key>" //
                        + "   <defaultValue>10.1</defaultValue>" //
                        + "    <values>" //
                        + "     <float>10.1</float><float>10.2</float><float>10.3</float>" //
                        + "    </values>" //
                        + "   <threshold>1.0E-2</threshold>" //
                        + "  </floatParameterFilter>" //
                        + " </parameterFilters>" //
                        + " <gutter>20</gutter>" //
                        + "</GeoServerLayer>";

        final String url = "gwc/rest/layers/" + layerName + ".xml";

        MockHttpServletResponse response = super.putAsServletResponse(url, xml, "text/xml");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        assertTrue(mediator.tileLayerExists(layerName));
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) mediator.getTileLayerByName(layerName);
        GeoServerTileLayerInfo info = tileLayer.getInfo();
        assertEquals(20, info.getGutter());
        assertEquals(2, tileLayer.getGridSubsets().size());
        assertTrue(tileLayer.getGridSubsets().contains("GoogleCRS84Quad"));
        assertTrue(tileLayer.getGridSubsets().contains("EPSG:4326"));
        assertEquals(ImmutableSet.of("image/png8"), info.getMimeFormats());
        assertEquals(9, info.getMetaTilingX());
        assertEquals(6, info.getMetaTilingY());

        List<ParameterFilter> filters = Lists.newArrayList(info.getParameterFilters());
        assertEquals(3, filters.size()); // Float, String, and a Style filter that should replace
        // the old String style filter.

        FloatParameterFilter floatFilter = null;
        StringParameterFilter stringFilter = null;
        StyleParameterFilter styleFilter = null;

        for (ParameterFilter filter : filters) {
            if (filter instanceof FloatParameterFilter) floatFilter = (FloatParameterFilter) filter;
            if (filter instanceof StringParameterFilter)
                stringFilter = (StringParameterFilter) filter;
            if (filter instanceof StyleParameterFilter) styleFilter = (StyleParameterFilter) filter;
        }

        assertNotNull(floatFilter);
        assertNotNull(stringFilter);
        assertNotNull(styleFilter);

        assertEquals("ELEVATION", floatFilter.getKey());
        assertEquals("10.1", floatFilter.getDefaultValue());
        assertEquals(1.0E-2f, floatFilter.getThreshold(), 0d);
        assertEquals(
                ImmutableList.of(Float.valueOf(10.1f), Float.valueOf(10.2f), Float.valueOf(10.3f)),
                floatFilter.getValues());

        assertEquals("BGCOLOR", stringFilter.getKey());
        assertEquals("0xFFFFFF", stringFilter.getDefaultValue());
        assertEquals(ImmutableList.of("0x000000", "0x888888"), stringFilter.getLegalValues());

        assertEquals("STYLES", styleFilter.getKey());
    }

    @Test
    public void testPutStyleParameterFilter() throws Exception {
        final String layerName = getLayerId(MockData.LAKES);

        final GWC mediator = GWC.get();
        assertTrue(mediator.tileLayerExists(layerName));
        mediator.removeTileLayers(Lists.newArrayList(layerName));
        assertFalse(mediator.tileLayerExists(layerName));

        final String xml =
                "<GeoServerLayer>" //
                        + " <enabled>true</enabled>" //
                        + " <name>"
                        + layerName
                        + "</name>" //
                        + " <mimeFormats><string>image/png8</string></mimeFormats>" //
                        + " <gridSubsets>" //
                        + "  <gridSubset><gridSetName>GoogleCRS84Quad</gridSetName></gridSubset>" //
                        + "  <gridSubset><gridSetName>EPSG:4326</gridSetName></gridSubset>" //
                        + " </gridSubsets>" //
                        + " <metaWidthHeight><int>9</int><int>6</int></metaWidthHeight>" //
                        + " <parameterFilters>" //
                        + "  <styleParameterFilter>" //
                        + "   <key>STYLES</key>" //
                        + "   <defaultValue>capitals</defaultValue>" //
                        + "   <allowedStyles><string>points</string><string>bergs</string></allowedStyles>" //
                        + "  </styleParameterFilter>" //
                        + " </parameterFilters>" //
                        + " <gutter>20</gutter>" //
                        + "</GeoServerLayer>";

        final String url = "gwc/rest/layers/" + layerName + ".xml";

        MockHttpServletResponse response = super.putAsServletResponse(url, xml, "text/xml");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        assertTrue(mediator.tileLayerExists(layerName));
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) mediator.getTileLayerByName(layerName);
        GeoServerTileLayerInfo info = tileLayer.getInfo();
        assertEquals(20, info.getGutter());
        assertEquals(2, tileLayer.getGridSubsets().size());
        assertTrue(tileLayer.getGridSubsets().contains("GoogleCRS84Quad"));
        assertTrue(tileLayer.getGridSubsets().contains("EPSG:4326"));
        assertEquals(ImmutableSet.of("image/png8"), info.getMimeFormats());
        assertEquals(9, info.getMetaTilingX());
        assertEquals(6, info.getMetaTilingY());

        List<ParameterFilter> filters = Lists.newArrayList(info.getParameterFilters());
        assertEquals(1, filters.size());

        StyleParameterFilter styleFilter = (StyleParameterFilter) filters.get(0);

        assertEquals("STYLES", styleFilter.getKey());
        assertEquals("capitals", styleFilter.getDefaultValue());
        assertEquals(ImmutableSet.of("points", "bergs"), styleFilter.getStyles());
    }

    private MockHttpServletResponse putLayer(String url, String id, String name) throws Exception {
        String xml =
                "<GeoServerLayer>" //
                        + "  <id>"
                        + id
                        + "</id>" //
                        + "  <enabled>true</enabled>" //
                        + "  <name>"
                        + name
                        + "</name>" //
                        + "  <mimeFormats>" //
                        + "    <string>image/jpeg</string>" //
                        + "  </mimeFormats>" //
                        + "  <gridSubsets>" //
                        + "    <gridSubset>" //
                        + "     <gridSetName>EPSG:900913</gridSetName>" //
                        + "    </gridSubset>" //
                        + "    <gridSubset>" //
                        + "     <gridSetName>EPSG:4326</gridSetName>" //
                        + "    </gridSubset>" //
                        + "  </gridSubsets>" //
                        + "  <metaWidthHeight>" //
                        + "    <int>4</int>" //
                        + "    <int>4</int>" //
                        + "  </metaWidthHeight>" //
                        + "  <autoCacheStyles>true</autoCacheStyles>" //
                        + "</GeoServerLayer>";

        final String contentType = "text/xml";
        MockHttpServletResponse response = super.putAsServletResponse(url, xml, contentType);
        return response;
    }

    @Test
    public void testDelete() throws Exception {
        final String layerName = getLayerId(MockData.BRIDGES);
        final GWC mediator = GWC.get();

        assertTrue(mediator.tileLayerExists(layerName));

        final String url = "gwc/rest/layers/" + layerName + ".xml";
        MockHttpServletResponse response = super.deleteAsServletResponse(url);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertFalse(mediator.tileLayerExists(layerName));
    }

    @Test
    public void testDeleteNonExistentLayer() throws Exception {

        final String url = "gwc/rest/layers/badLayerName.xml";
        MockHttpServletResponse response = super.deleteAsServletResponse(url);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        // See GWC's TileLayerRestlet
        assertEquals("Unknown layer: badLayerName", response.getContentAsString());
    }

    @Test
    public void testPost() throws Exception {
        final String layerName = getLayerId(MockData.ROAD_SEGMENTS);

        final GWC mediator = GWC.get();
        assertTrue(mediator.tileLayerExists(layerName));

        final String xml =
                "<GeoServerLayer>" //
                        + " <enabled>true</enabled>" //
                        + " <name>"
                        + layerName
                        + "</name>" //
                        + " <mimeFormats><string>image/png8</string></mimeFormats>" //
                        + " <gridSubsets>" //
                        + "  <gridSubset><gridSetName>GoogleCRS84Quad</gridSetName></gridSubset>" //
                        + "  <gridSubset><gridSetName>EPSG:4326</gridSetName></gridSubset>" //
                        + " </gridSubsets>" //
                        + " <metaWidthHeight><int>9</int><int>6</int></metaWidthHeight>" //
                        + " <parameterFilters>" //
                        + "  <styleParameterFilter>" //
                        + "   <key>STYLES</key>" //
                        + "   <defaultValue>capitals</defaultValue>" //
                        + "   <allowedStyles><string>burg</string><string>point</string></allowedStyles>" //
                        + "  </styleParameterFilter>" //
                        + "  <floatParameterFilter>" //
                        + "   <key>ELEVATION</key>" //
                        + "   <defaultValue>10.1</defaultValue>" //
                        + "    <values>" //
                        + "     <float>10.1</float><float>10.2</float><float>10.3</float>" //
                        + "    </values>" //
                        + "   <threshold>1.0E-2</threshold>" //
                        + "  </floatParameterFilter>" //
                        + " </parameterFilters>" //
                        + " <gutter>20</gutter>" //
                        + "</GeoServerLayer>";

        final String url = "gwc/rest/layers/" + layerName + ".xml";

        MockHttpServletResponse response = super.postAsServletResponse(url, xml, "text/xml");

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        assertTrue(mediator.tileLayerExists(layerName));
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) mediator.getTileLayerByName(layerName);
        GeoServerTileLayerInfo info = tileLayer.getInfo();
        assertEquals(20, info.getGutter());
        assertEquals(2, tileLayer.getGridSubsets().size());
        assertTrue(tileLayer.getGridSubsets().contains("GoogleCRS84Quad"));
        assertTrue(tileLayer.getGridSubsets().contains("EPSG:4326"));
        assertEquals(ImmutableSet.of("image/png8"), info.getMimeFormats());
        assertEquals(9, info.getMetaTilingX());
        assertEquals(6, info.getMetaTilingY());

        List<ParameterFilter> filters = Lists.newArrayList(info.getParameterFilters());
        assertEquals(2, filters.size());

        FloatParameterFilter floatFilter = null;
        StyleParameterFilter styleFilter = null;

        for (ParameterFilter filter : filters) {
            if (filter instanceof FloatParameterFilter) floatFilter = (FloatParameterFilter) filter;
            if (filter instanceof StyleParameterFilter) styleFilter = (StyleParameterFilter) filter;
        }

        assertNotNull(floatFilter);
        assertNotNull(styleFilter);

        assertEquals("ELEVATION", floatFilter.getKey());
        assertEquals("10.1", floatFilter.getDefaultValue());
        assertEquals(1.0E-2f, floatFilter.getThreshold(), 0d);
        assertEquals(
                ImmutableList.of(Float.valueOf(10.1f), Float.valueOf(10.2f), Float.valueOf(10.3f)),
                floatFilter.getValues());

        assertEquals("STYLES", styleFilter.getKey());
        assertEquals("capitals", styleFilter.getDefaultValue());
        assertEquals(ImmutableSet.of("burg", "point"), styleFilter.getStyles());
    }

    @Test
    public void testPostLegacyAutoStyles() throws Exception {
        final String layerName = getLayerId(MockData.ROAD_SEGMENTS);

        final GWC mediator = GWC.get();
        assertTrue(mediator.tileLayerExists(layerName));

        final String url = "gwc/rest/layers/" + layerName + ".xml";
        {
            final String xml =
                    "<GeoServerLayer>" //
                            + " <name>"
                            + layerName
                            + "</name>" //
                            + " <autoCacheStyles>true</autoCacheStyles>"
                            + "</GeoServerLayer>";

            MockHttpServletResponse response = super.postAsServletResponse(url, xml, "text/xml");

            assertEquals(HttpServletResponse.SC_OK, response.getStatus());

            GeoServerTileLayer tileLayer =
                    (GeoServerTileLayer) mediator.getTileLayerByName(layerName);
            GeoServerTileLayerInfo info = tileLayer.getInfo();

            List<ParameterFilter> filters = Lists.newArrayList(info.getParameterFilters());
            assertThat(
                    filters,
                    contains(
                            allOf(
                                    Matchers.<ParameterFilter>hasProperty("key", is("STYLES")),
                                    isA(
                                            (Class<ParameterFilter>)
                                                    StyleParameterFilter.class.asSubclass(
                                                            ParameterFilter.class)))));
        }
        {
            final String xml =
                    "<GeoServerLayer>" //
                            + " <name>"
                            + layerName
                            + "</name>" //
                            + " <autoCacheStyles>false</autoCacheStyles>"
                            + "</GeoServerLayer>";

            MockHttpServletResponse response = super.postAsServletResponse(url, xml, "text/xml");

            assertEquals(HttpServletResponse.SC_OK, response.getStatus());

            GeoServerTileLayer tileLayer =
                    (GeoServerTileLayer) mediator.getTileLayerByName(layerName);
            GeoServerTileLayerInfo info = tileLayer.getInfo();

            List<ParameterFilter> filters = Lists.newArrayList(info.getParameterFilters());
            assertThat(
                    filters,
                    not(
                            contains(
                                    allOf(
                                            Matchers.<ParameterFilter>hasProperty(
                                                    "key", is("STYLES")),
                                            isA(
                                                    (Class<ParameterFilter>)
                                                            StyleParameterFilter.class.asSubclass(
                                                                    ParameterFilter.class))))));
        }
    }

    @Test
    public void testGetSeedHtml() throws Exception {
        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String url = "gwc/rest/seed/" + layerName;
        final String id = getCatalog().getLayerByName(layerName).getId();

        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatus());
        assertTrue(sr.getContentType(), sr.getContentType().startsWith("text/html"));
    }

    @Test
    public void testPostSeedHtmlForm() throws Exception {
        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String url = "gwc/rest/seed/" + layerName;
        final String id = getCatalog().getLayerByName(layerName).getId();

        final String formData =
                "threadCount=01&type=seed&gridSetId=EPSG%3A4326&tileFormat=image%2Fpng&zoomStart=00&zoomStop=12&minX=&minY=&maxX=&maxY=";

        // Manually construct request and wrap in BufferedRequestWrapper so that the form data gets
        // parsed as parameters
        MockHttpServletRequest request = createRequest(url);
        request.setMethod("POST");
        request.setContentType("application/x-www-form-urlencoded");
        request.setContent(formData.getBytes("UTF-8"));

        BufferedRequestWrapper wrapper =
                new BufferedRequestWrapper(request, "UTF-8", formData.getBytes("UTF-8"));

        MockHttpServletResponse sr = dispatch(wrapper);

        assertEquals(200, sr.getStatus());
        assertSeedJob(layerName);
    }

    @Test
    public void testGetSeedJson() throws Exception {
        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String url = "gwc/rest/seed/" + layerName + ".json";
        final String id = getCatalog().getLayerByName(layerName).getId();

        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatus());
        assertTrue(sr.getContentType(), sr.getContentType().startsWith("application/json"));
        JSONObject json = (JSONObject) getAsJSON(url);
        assertNotNull(json.getJSONArray("long-array-array"));
    }

    @Test
    public void testPostSeedXml() throws Exception {
        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String url = "gwc/rest/seed/" + layerName + ".xml";
        final String id = getCatalog().getLayerByName(layerName).getId();

        final String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<seedRequest>\n"
                        + "  <name>"
                        + layerName
                        + "</name>\n"
                        + "  <gridSetId>EPSG:4326</gridSetId>\n"
                        + "  <zoomStart>0</zoomStart>\n"
                        + "  <zoomStop>12</zoomStop>\n"
                        + "  <format>image/png</format>\n"
                        + "  <type>seed</type>\n"
                        + "  <threadCount>1</threadCount>\n"
                        + "</seedRequest>";

        MockHttpServletResponse sr = postAsServletResponse(url, xml);
        assertEquals(200, sr.getStatus());
        assertSeedJob(layerName);
    }

    @Test
    public void testPostSeedJson() throws Exception {
        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String url = "gwc/rest/seed/" + layerName + ".json";
        final String id = getCatalog().getLayerByName(layerName).getId();

        final String json =
                "{ \"seedRequest\": {\n"
                        + "  \"name\": \""
                        + layerName
                        + "\",\n"
                        + "  \"gridSetId\": \"EPSG:4326\",\n"
                        + "  \"zoomStart\": 0,\n"
                        + "  \"zoomStop\": 12,\n"
                        + "  \"type\": \"seed\",\n"
                        + "  \"threadCount\": 1,\n"
                        + "}}";

        MockHttpServletResponse sr = postAsServletResponse(url, json);
        assertEquals(200, sr.getStatus());
        assertSeedJob(layerName);
    }

    public void assertSeedJob(String layerName) throws Exception {
        final String url = "gwc/rest/seed/" + layerName + ".json";

        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatus());
        assertTrue(sr.getContentType(), sr.getContentType().startsWith("application/json"));
        JSONObject json = (JSONObject) getAsJSON(url);

        JSONArray jsonArr = json.getJSONArray("long-array-array");
        assertNotNull(jsonArr);
        assertTrue(jsonArr.size() > 0);
    }

    @Test
    public void testPostReloadHtmlForm() throws Exception {
        final String url = "gwc/rest/reload";

        final String formData = "reload_configuration=1";

        // Manually construct request and wrap in BufferedRequestWrapper so that the form data gets
        // parsed as parameters
        MockHttpServletRequest request = createRequest(url);
        request.setMethod("POST");
        request.setContentType("application/x-www-form-urlencoded");
        request.setContent(formData.getBytes("UTF-8"));

        BufferedRequestWrapper wrapper =
                new BufferedRequestWrapper(request, "UTF-8", formData.getBytes("UTF-8"));

        MockHttpServletResponse sr = dispatch(wrapper);

        assertEquals(200, sr.getStatus());
    }
}
