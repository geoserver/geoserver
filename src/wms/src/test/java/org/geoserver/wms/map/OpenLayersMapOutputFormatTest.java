/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.TestHttpClientRule;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.LayerGroupStyleImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.logging.TestAppender;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.http.MockHttpClient;
import org.geoserver.test.http.MockHttpResponse;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class OpenLayersMapOutputFormatTest extends WMSTestSupport {

    Pattern lookForEscapedParam =
            Pattern.compile(Pattern.quote("\"</script><script>alert('x-scripted');</script><script>\": 'foo'"));

    @Rule
    public TestHttpClientRule clientMocker = new TestHttpClientRule();

    @Override
    protected String getLogConfiguration() {
        // needed for a test on logging capabilities
        GeoServerResourceLoader loader = new GeoServerResourceLoader(testData.getDataDirectoryRoot());

        Resource resource = loader.get("logs/OL_LOGGING.properties");
        if (resource.getType() == Resource.Type.UNDEFINED) {
            try {
                loader.copyFromClassPath("/OL_LOGGING.properties", "logs/OL_LOGGING.properties");
            } catch (IOException e) {
                LOGGER.fine("Unable to configure with OL_LOGGING");
                return "TEST_LOGGING";
            }
        }
        return "OL_LOGGING";
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // get default workspace info
        WorkspaceInfo workspaceInfo = getCatalog().getWorkspaceByName(MockData.DEFAULT_PREFIX);
        // create static raster store
        StoreInfo store = createStaticRasterStore(workspaceInfo);
        // create static raster layer
        NamespaceInfo nameSpace = getCatalog().getNamespaceByPrefix(MockData.DEFAULT_PREFIX);
        createStaticRasterLayer(nameSpace, store, "staticRaster");
    }

    /**
     * Test for GEOS-5318: xss vulnerability when a weird parameter is added to the request (something like:
     * %3C%2Fscript% 3E%3Cscript%3Ealert%28%27x-scripted%27%29%3C%2Fscript%3E%3Cscript%3E=foo) the causes js code
     * execution.
     */
    @Test
    public void testXssFix() throws Exception {

        Catalog catalog = getCatalog();
        final FeatureSource fs = catalog.getFeatureTypeByName(
                        MockData.BASIC_POLYGONS.getPrefix(), MockData.BASIC_POLYGONS.getLocalPart())
                .getFeatureSource(null, null);

        final Envelope env = fs.getBounds();

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        GetMapRequest request = createGetMapRequest(MockData.BASIC_POLYGONS);
        request.getRawKvp().put("</script><script>alert('x-scripted');</script><script>", "foo");
        request.getRawKvp().put("25064;ALERT(1)//419", "1");
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName("Default");
        Style basicStyle = styleByName.getStyle();
        FeatureLayer layer = new FeatureLayer(fs, basicStyle);
        layer.setTitle("Title</foo");
        map.addLayer(layer);
        request.setFormat("application/openlayers");

        StyleInfo otherStyle = new StyleInfoImpl(null);
        otherStyle.setName("style<>");
        try {
            request.getLayers().get(0).getLayerInfo().getStyles().add(otherStyle);
            String htmlDoc = getAsHTML(map);
            // check that weird param is correctly encoded to avoid js code execution
            assertThat(
                    htmlDoc,
                    containsString(
                            "&lt;/script&gt;&lt;script&gt;alert(&#39;x-scripted&#39;);&lt;/script&gt;&lt;script&gt;"));
            assertThat(htmlDoc, containsString("25064;ALERT(1)//419"));
            assertThat(htmlDoc, not(containsString(layer.getTitle())));
            assertThat(htmlDoc, containsString("Title&lt;/foo"));
            assertThat(htmlDoc, not(containsString(otherStyle.getName())));
            assertThat(htmlDoc, containsString("style&lt;&gt;"));
        } finally {
            request.getLayers().get(0).getLayerInfo().getStyles().remove(otherStyle);
        }
    }

    @Test
    public void testRastersFilteringCapabilities() throws Exception {
        // static raster layer supports filtering
        MockHttpServletResponse response =
                getAsServletResponse("wms?service=WMS&version=1.1.0&request=GetMap&layers=gs:staticRaster"
                        + "&styles=&bbox=0.2372206885127698,40.562080748421806,"
                        + "14.592757149389236,44.55808294568743&width=768&height=330"
                        + "&srs=EPSG:4326&format=application/openlayers");
        String content = response.getContentAsString();
        assertThat(content, containsString("<input type=\"hidden\" id=\"supportsFiltering\" value=\"true\"/>"));
        // world raster layer doesn't support filtering
        response = getAsServletResponse("wms?service=WMS&version=1.1.0&request=GetMap&layers=wcs:World"
                + "&styles=&bbox=0.2372206885127698,40.562080748421806,"
                + "14.592757149389236,44.55808294568743&width=768&height=330"
                + "&srs=EPSG:4326&format=application/openlayers");
        content = response.getContentAsString();
        assertThat(content, containsString("<input type=\"hidden\" id=\"supportsFiltering\" value=\"false\"/>"));

        // if at least one layer supports filtering, overall filtering should be supported
        response = getAsServletResponse("wms?service=WMS&version=1.1.0&request=GetMap&layers=wcs:World,gs:staticRaster"
                + "&styles=&bbox=0.2372206885127698,40.562080748421806,"
                + "14.592757149389236,44.55808294568743&width=768&height=330"
                + "&srs=EPSG:4326&format=application/openlayers");
        content = response.getContentAsString();
        assertThat(content, containsString("<input type=\"hidden\" id=\"supportsFiltering\" value=\"true\"/>"));
    }

    @Test
    public void testWMTSFilteringCapabilities() throws Exception {

        // Create a cascading layer
        createWMTSCatalogStuff();

        // wmts by itself should not support filtering
        MockHttpServletResponse response =
                getAsServletResponse("wms?service=WMS&version=1.1.0&request=GetMap&layers=gs:wmtslayername"
                        + "&styles=&bbox=0.2372206885127698,40.562080748421806,"
                        + "14.592757149389236,44.55808294568743&width=768&height=330"
                        + "&srs=EPSG:4326&format=application/openlayers");
        String content = response.getContentAsString();
        assertThat(content, containsString("<input type=\"hidden\" id=\"supportsFiltering\" value=\"false\"/>"));

        // wmts along with filterable layer should support filtering
        response = getAsServletResponse(
                "wms?service=WMS&version=1.1.0&request=GetMap&layers=gs:wmtslayername,gs:staticRaster"
                        + "&styles=&bbox=0.2372206885127698,40.562080748421806,"
                        + "14.592757149389236,44.55808294568743&width=768&height=330"
                        + "&srs=EPSG:4326&format=application/openlayers");
        content = response.getContentAsString();
        assertThat(content, containsString("<input type=\"hidden\" id=\"supportsFiltering\" value=\"true\"/>"));
    }

    /** Helper method that creates a static raster store and adds it to the catalog. */
    private StoreInfo createStaticRasterStore(WorkspaceInfo workspace) {
        Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getFactory().createCoverageStore();
        store.setWorkspace(workspace);
        store.setType("StaticRaster");
        store.setEnabled(true);
        store.setName("StaticRaster");
        // some fictive URL
        store.setURL("http://127.0.0.1:geoserver");
        // add the store to the catalog
        catalog.add(store);
        return store;
    }

    /** Helper method that creates a static WMTS store and related layer and adds it to the catalog. */
    private StoreInfo createWMTSCatalogStuff() throws MalformedURLException, IOException {
        // use a local mock capabilities
        String capabilities =
                clientMocker.getServer() + "/geoserver/gwc?REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS";
        MockHttpClient client = new MockHttpClient();
        client.expectGet(
                new URL(capabilities), new MockHttpResponse(getClass().getResource("/nasa.getcapa.xml"), "text/xml"));
        clientMocker.bind(client, capabilities);

        Catalog catalog = getCatalog();

        WorkspaceInfo workspace = getCatalog().getWorkspaceByName(MockData.DEFAULT_PREFIX);
        NamespaceInfo nameSpace = getCatalog().getNamespaceByPrefix(MockData.DEFAULT_PREFIX);

        // Store
        WMTSStoreInfo store = catalog.getFactory().createWebMapTileServer();
        store.setWorkspace(workspace);
        store.setType("WMTS");
        store.setEnabled(true);
        store.setName("wmts");
        store.setCapabilitiesURL(capabilities);
        catalog.add(store);

        // Resource
        WMTSLayerInfo resource = catalog.getFactory().createWMTSLayer();
        resource.setNamespace(nameSpace);
        resource.setName("wmtslayername");
        resource.setNativeName("AMSR2_Snow_Water_Equivalent");
        resource.setEnabled(true);
        resource.setStore(store);
        catalog.add(resource);

        // Layer
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setResource(resource);
        layer.setEnabled(true);
        layer.setName("wmtslayername");
        catalog.add(layer);

        return store;
    }

    /** Helper method that creates a static raster layer and adds it to the catalog. */
    private void createStaticRasterLayer(NamespaceInfo namespace, StoreInfo store, String layerName) {
        Catalog catalog = getCatalog();
        // creating the coverage info
        CoverageInfoImpl coverageInfo = new CoverageInfoImpl(catalog);
        coverageInfo.setNamespace(namespace);
        coverageInfo.setName(layerName);
        coverageInfo.setNativeCoverageName(layerName);
        coverageInfo.setStore(store);
        // creating the layer
        LayerInfoImpl layer = new LayerInfoImpl();
        layer.setResource(coverageInfo);
        layer.setEnabled(true);
        layer.setName(layerName);
        // set the layers styles
        layer.setDefaultStyle(catalog.getStyleByName("raster"));
        // set layer CRS and native CRS
        coverageInfo.setNativeCRS(DefaultGeographicCRS.WGS84);
        coverageInfo.setSRS("EPSG:4326");
        // saving everything
        catalog.add(coverageInfo);
        catalog.add(layer);
    }

    /**
     * Test for GEOS-8178: OpenLayersOutputFormat NoSuchAuthorityCodeExceptions being thrown due to malformed URN codes.
     *
     * <p>Exception is thrown when decoding CRS in isWms13FlippedCRS which is called by produceMap, test uses produceMap
     * and reads the resulting output steam to ensure "yx: true" is returned for EPSG:4326, output is false before fix
     */
    @Test
    public void testUrnCodeFix() throws Exception {

        Catalog catalog = getCatalog();
        final FeatureSource fs = catalog.getFeatureTypeByName(
                        MockData.BASIC_POLYGONS.getPrefix(), MockData.BASIC_POLYGONS.getLocalPart())
                .getFeatureSource(null, null);

        final Envelope env = fs.getBounds();

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        GetMapRequest request = createGetMapRequest(MockData.BASIC_POLYGONS);
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");

        request.setCrs(crs);

        final WMSMapContent map = new WMSMapContent();
        map.setRequest(request);
        request.setFormat("application/openlayers");

        String htmlDoc = getAsHTML(map);
        assertThat(htmlDoc, containsString("<input type=\"hidden\" id=\"SRS\" value=\"EPSG:4326\"/>"));
        assertThat(htmlDoc, containsString("<input type=\"hidden\" id=\"yx\" value=\"true\"/>"));
    }

    @Test
    public void testOL3vsOL2() throws Exception {
        // the base request
        String path = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.BASIC_POLYGONS)
                + "&styles=&bbox=-180,-90,180,90&width=768&height=330"
                + "&srs=EPSG:4326&format=";
        final String firefoxAgent = "Firefox 40.1";
        String ie8Agent = "MSIE 8.";

        // generic request on browser supporting OL3
        String contentFirefox = getResponseContent(
                path + "application/openlayers", firefoxAgent, getBaseMimeType(OpenLayers3MapOutputFormat.MIME_TYPE));
        assertThat(contentFirefox, containsString("openlayers3/ol.js"));

        // generic request on browser not supporting OL3
        String contentIE8 = getResponseContent(
                path + "application/openlayers", ie8Agent, getBaseMimeType(OpenLayers2MapOutputFormat.MIME_TYPE));
        assertThat(contentIE8, containsString("OpenLayers.js"));

        // ask explicitly for OL2
        String contentOL2 = getResponseContent(
                path + "application/openlayers2", firefoxAgent, getBaseMimeType(OpenLayers2MapOutputFormat.MIME_TYPE));
        assertThat(contentOL2, containsString("OpenLayers.js"));

        // ask explicitly for OL3
        String contentOL3 = getResponseContent(
                path + "application/openlayers3", firefoxAgent, getBaseMimeType(OpenLayers3MapOutputFormat.MIME_TYPE));
        assertThat(contentOL3, containsString("openlayers3/ol.js"));

        // ask explicitly for OL3 on a non supporting browser
        String exception = getResponseContent(path + "application/openlayers3", ie8Agent, "application/vnd.ogc.se_xml");
        assertThat(exception, containsString("not supported"));
    }

    public String getResponseContent(String path, String userAgent, String expectedMimeType) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("GET");
        request.setContent(new byte[] {});
        if (userAgent != null) {
            request.addHeader("USER-AGENT", userAgent);
        }
        MockHttpServletResponse response = dispatch(request);
        assertEquals(expectedMimeType, getBaseMimeType(response.getContentType()));
        return response.getContentAsString();
    }

    String getAsHTML(WMSMapContent map) throws IOException {
        OpenLayersMapOutputFormat mapProducer =
                GeoServerExtensions.extensions(OpenLayersMapOutputFormat.class).get(0);
        RawMap rawMap = mapProducer.produceMap(map);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        rawMap.writeTo(bos);
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    String getAsHTMLOL3(WMSMapContent map) throws IOException {
        OpenLayers3MapOutputFormat mapProducer =
                GeoServerExtensions.extensions(OpenLayers3MapOutputFormat.class).get(0);
        RawMap rawMap = mapProducer.produceMap(map);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        rawMap.writeTo(bos);
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    public void testExceptionsInImage() throws Exception {
        // the base request
        String path = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.BASIC_POLYGONS)
                + "&styles=&bbox=-180,-90,180,90&width=768&height=330"
                + "&srs=EPSG:4326&format=application/openlayers";

        String html = getAsString(path);
        assertThat(
                html,
                containsString(
                        "<input type=\"hidden\" class=\"param\" title=\"EXCEPTIONS\" value=\"application/vnd.ogc.se_inimage\"/>"));
    }

    @Test
    public void testExceptionsXML() throws Exception {
        // the base request
        String path = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.BASIC_POLYGONS)
                + "&styles=&bbox=-180,-90,180,90&width=768&height=330"
                + "&srs=EPSG:4326&format=application/openlayers"
                + "&exceptions=application/vnd.ogc.se_xml";

        String html = getAsString(path);
        assertThat(
                html,
                containsString(
                        "<input type=\"hidden\" class=\"param\" title=\"EXCEPTIONS\" value=\"application/vnd.ogc.se_xml\"/>"));
        assertThat(
                html,
                not(
                        containsString(
                                "<input type=\"hidden\" class=\"param\" title=\"EXCEPTIONS\" value=\"application/vnd.ogc.se_inimage\"/>")));
    }

    @Test
    public void testXssOL3() throws Exception {

        Catalog catalog = getCatalog();
        final FeatureSource fs = catalog.getFeatureTypeByName(
                        MockData.BASIC_POLYGONS.getPrefix(), MockData.BASIC_POLYGONS.getLocalPart())
                .getFeatureSource(null, null);

        final Envelope env = fs.getBounds();

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        GetMapRequest request = createGetMapRequest(MockData.BASIC_POLYGONS);
        request.putHttpRequestHeader("USER-AGENT", "Firefox 40.1");
        request.getRawKvp().put("</script><script>alert('x-scripted');</script><script>", "foo");
        request.getRawKvp().put("25064;ALERT(1)//419", "1");
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName("Default");
        Style basicStyle = styleByName.getStyle();
        FeatureLayer layer = new FeatureLayer(fs, basicStyle);
        layer.setTitle("Title");
        map.addLayer(layer);
        request.setFormat("application/openlayers3");

        StyleInfo otherStyle = new StyleInfoImpl(null);
        otherStyle.setName("style<>");
        try {
            request.getLayers().get(0).getLayerInfo().getStyles().add(otherStyle);
            String htmlDoc = getAsHTMLOL3(map);
            // check that weird param is correctly encoded to avoid js code execution
            assertThat(
                    htmlDoc,
                    containsString(
                            "&lt;/script&gt;&lt;script&gt;alert(&#39;x-scripted&#39;);&lt;/script&gt;&lt;script&gt;"));
            assertThat(htmlDoc, containsString("25064;ALERT(1)//419"));
            assertThat(htmlDoc, not(containsString(otherStyle.getName())));
            assertThat(htmlDoc, containsString("style&lt;&gt;"));
        } finally {
            request.getLayers().get(0).getLayerInfo().getStyles().remove(otherStyle);
        }
    }

    @Test
    public void testLayerGroupStylesInDropdown() throws Exception {
        // tests that layer groups style are available from the styles dropdown in ol preview
        LayerGroupInfo group = null;
        Catalog catalog = getCatalog();
        try {
            createLakesPlacesLayerGroup(catalog, "lakes_and_places_group", LayerGroupInfo.Mode.SINGLE, null);
            group = catalog.getLayerGroupByName("lakes_and_places_group");
            LayerGroupStyle groupStyle = new LayerGroupStyleImpl();
            StyleInfo styleName = new StyleInfoImpl(getCatalog());
            styleName.setName("nature-style");
            groupStyle.setName(styleName);
            groupStyle.getLayers().add(getCatalog().getLayerByName("cite:Forests"));
            groupStyle.getLayers().add(getCatalog().getLayerByName("cite:Lakes"));
            groupStyle.getStyles().add(null);
            groupStyle.getStyles().add(null);
            group.getLayerGroupStyles().add(groupStyle);
            catalog.save(group);
            String url = "wms?LAYERS="
                    + group.getName()
                    + "&STYLES=&format=application/openlayers"
                    + "&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG%3A4326&WIDTH=256&HEIGHT=256&bbox=-0.002,-0.003,0.005,0.002";
            MockHttpServletResponse response = getAsServletResponse(url);
            String content = response.getContentAsString();
            assertTrue(content.contains("<option value=\"nature-style\">nature-style</option>"));
        } finally {
            if (group != null) catalog.remove(group);
        }
    }

    @Test
    public void testAutoCodeLogsErrors() throws Exception {
        try (TestAppender appender = TestAppender.createAppender("testAutoCodeLogsErrors", null)) {
            appender.startRecording("org.geoserver.wms.map");

            GetMapRequest request = createGetMapRequest(MockData.BASIC_POLYGONS);
            CoordinateReferenceSystem crs = CRS.decode("AUTO:42003,9001,-20,-45");
            request.setCrs(crs);
            request.setBbox(new Envelope(-3_000_000, 3_000_000, -3_000_000, 3_000_000));

            final WMSMapContent map = new WMSMapContent();
            map.setRequest(request);
            request.setFormat("application/openlayers");

            String htmlDoc = getAsHTML(map);
            assertThat(htmlDoc, containsString("<input type=\"hidden\" id=\"SRS\" value=\"EPSG:4326\"/>"));
            assertThat(htmlDoc, containsString("<input type=\"hidden\" id=\"yx\" value=\"false\"/>"));

            appender.assertFalse("Error was logged", "Failed to determine CRS axis order");
            appender.stopRecording("org.geoserver.wms.map");
        }
    }

    @Test
    public void testRemovedInlineJavaScriptOL2() throws Exception {
        WMSMapContent map = new WMSMapContent();
        map.setRequest(createGetMapRequest(MockData.BASIC_POLYGONS));
        String htmlDoc = getAsHTML(map);
        assertThat(htmlDoc, containsString("<script src=\""));
        assertThat(htmlDoc, not(containsString("<script defer=\"defer\" type=\"text/javascript\">")));
        assertThat(htmlDoc, not(containsString(" onload=")));
        assertThat(htmlDoc, not(containsString(" onchange=")));
        assertThat(htmlDoc, not(containsString(" onClick=")));
    }

    @Test
    public void testRemovedInlineJavaScriptOL3() throws Exception {
        WMSMapContent map = new WMSMapContent();
        map.setRequest(createGetMapRequest(MockData.BASIC_POLYGONS));
        map.getRequest().putHttpRequestHeader("USER-AGENT", "Firefox 40.1");
        String htmlDoc = getAsHTMLOL3(map);
        assertThat(htmlDoc, containsString("<script src=\""));
        assertThat(htmlDoc, not(containsString("<script type=\"text/javascript\">")));
        assertThat(htmlDoc, not(containsString(" onchange=")));
        assertThat(htmlDoc, not(containsString(" onClick=")));
    }
}
