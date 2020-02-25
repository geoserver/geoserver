/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.platform.resource.Resource;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class LegendCapabilitiesTest extends WMSTestSupport {

    private static final String CAPABILITIES_REQUEST = "wms?request=getCapabilities&version=1.3.0";

    // Reusing layer and SLD files from another test; their content doesn't really matter.
    // What is important for this test is the legend info we are adding.
    private static final String LAYER_NAME = "watertemp";
    private static final String HTTP_LEGEND_LAYER = "watertemp_http_legend";
    private static final QName LAYER_QNAME =
            new QName(MockData.DEFAULT_URI, LAYER_NAME, MockData.DEFAULT_PREFIX);
    private static final QName LAYER_QNAME_HTP_LEGND =
            new QName(MockData.DEFAULT_URI, HTTP_LEGEND_LAYER, MockData.DEFAULT_PREFIX);
    private static final String LAYER_FILE = "custwatertemp.zip";
    private static final String STYLE_NAME = "temperature";
    private static final String STYLE_NAME_HTTP = "temperature_http_url";
    private static final String STYLE_FILE = "../temperature.sld";

    private static final int LEGEND_WIDTH = 22;
    private static final int LEGEND_HEIGHT = 22;
    private static final String LEGEND_FORMAT = "image/jpeg";
    private static final String IMAGE_URL = "legend.png";
    private static final String IMAGE_HTTP_URL = "http://some.url.com/legend.png";
    private static final String BASE = "src/test/resources/geoserver";

    private static final String LAYER_NAME_WS = "watertemp_ws";
    private static final QName LAYER_QNAME_WS =
            new QName(MockData.DEFAULT_URI, LAYER_NAME_WS, MockData.DEFAULT_PREFIX);
    private static final String STYLE_NAME_WS = "temperature_ws";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        LegendInfo legend = new LegendInfoImpl();
        legend.setWidth(LEGEND_WIDTH);
        legend.setHeight(LEGEND_HEIGHT);
        legend.setFormat(LEGEND_FORMAT);
        legend.setOnlineResource(IMAGE_URL);

        // add legend.png to styles directory
        Resource resource = getResourceLoader().get("styles/legend.png");
        getResourceLoader().copyFromClassPath("../legend.png", resource.file(), getClass());

        // add layer
        testData.addStyle(null, STYLE_NAME, STYLE_FILE, getClass(), getCatalog(), legend);
        Map<SystemTestData.LayerProperty, Object> propertyMap =
                new HashMap<SystemTestData.LayerProperty, Object>();
        propertyMap.put(LayerProperty.STYLE, STYLE_NAME);
        testData.addRasterLayer(
                LAYER_QNAME, LAYER_FILE, null, propertyMap, SystemTestData.class, getCatalog());

        // Test for workspaced legend graphic
        LegendInfo legendWs = new LegendInfoImpl();
        legendWs.setWidth(LEGEND_WIDTH);
        legendWs.setHeight(LEGEND_HEIGHT);
        legendWs.setFormat(LEGEND_FORMAT);
        legendWs.setOnlineResource(IMAGE_URL);

        // add legend.png to styles directory
        Resource resourceWs = getResourceLoader().get("workspaces/gs/styles/legend.png");
        getResourceLoader().copyFromClassPath("../legend.png", resourceWs.file(), getClass());

        // add layer
        WorkspaceInfo wsInfo = getCatalog().getWorkspaceByName("gs");
        testData.addStyle(wsInfo, STYLE_NAME_WS, STYLE_FILE, getClass(), getCatalog(), legendWs);
        Map<SystemTestData.LayerProperty, Object> propertyMapWs =
                new HashMap<SystemTestData.LayerProperty, Object>();
        propertyMapWs.put(LayerProperty.STYLE, STYLE_NAME_WS);
        testData.addRasterLayer(
                LAYER_QNAME_WS,
                LAYER_FILE,
                null,
                propertyMapWs,
                SystemTestData.class,
                getCatalog());

        addLayerWithHttpLegend(testData);

        // For global set-up
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl(BASE);
        getGeoServer().save(global);

        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        getGeoServer().save(wms);

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("", "http://www.opengis.net/wms");
        namespaces.put("wms", "http://www.opengis.net/wms");
        getTestData().registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    private void addLayerWithHttpLegend(SystemTestData testData) throws IOException {
        LegendInfo legend = new LegendInfoImpl();
        legend.setWidth(LEGEND_WIDTH);
        legend.setHeight(LEGEND_HEIGHT);
        legend.setFormat(LEGEND_FORMAT);
        legend.setOnlineResource(IMAGE_HTTP_URL);

        // add layer
        testData.addStyle(null, STYLE_NAME_HTTP, STYLE_FILE, getClass(), getCatalog(), legend);
        Map<SystemTestData.LayerProperty, Object> propertyMap =
                new HashMap<SystemTestData.LayerProperty, Object>();
        propertyMap.put(LayerProperty.STYLE, STYLE_NAME);
        testData.addRasterLayer(
                LAYER_QNAME_HTP_LEGND,
                LAYER_FILE,
                null,
                propertyMap,
                SystemTestData.class,
                getCatalog());
    }

    @Test
    public void testCapabilities() throws Exception {
        Document dom = dom(get(CAPABILITIES_REQUEST), false);
        // print(dom);
        // assert that legend resources are hidden behind a GetLegendGraphic request URL in all
        // three
        // cases
        String expectedGetLegendGraphicRequestURL =
                "/ows?service=WMS&request=GetLegendGraphic&format=image%2Fjpeg&width=20&height=20&layer=gs%3A"
                        + LAYER_NAME;
        String expectedGetLegendGraphicRequestURLWS =
                "/ows?service=WMS&request=GetLegendGraphic&format=image%2Fjpeg&width=20&height=20&layer=gs%3A"
                        + LAYER_NAME_WS;
        String expectedGetLegendGraphicRequestURLHttp =
                "/ows?service=WMS&request=GetLegendGraphic&format=image%2Fjpeg&width=20&height=20&layer=gs%3A"
                        + HTTP_LEGEND_LAYER;

        final String legendUrlPath =
                "//wms:Layer[wms:Name='gs:" + LAYER_NAME + "']/wms:Style/wms:LegendURL";

        // assert that legend resources are hidden behind a GetLegendGraphic request URL
        assertXpathEvaluatesTo(String.valueOf(LEGEND_WIDTH), legendUrlPath + "/@width", dom);
        assertXpathEvaluatesTo(String.valueOf(LEGEND_HEIGHT), legendUrlPath + "/@height", dom);
        assertXpathEvaluatesTo(LEGEND_FORMAT, legendUrlPath + "/wms:Format", dom);
        assertXpathEvaluatesTo(
                BASE + expectedGetLegendGraphicRequestURL,
                legendUrlPath + "/wms:OnlineResource/@xlink:href",
                dom);

        final String legendUrlPathWs =
                "//wms:Layer[wms:Name='gs:" + LAYER_NAME_WS + "']/wms:Style/wms:LegendURL";

        // assert that legend resources are hidden behind a GetLegendGraphic request URL
        assertXpathEvaluatesTo(String.valueOf(LEGEND_WIDTH), legendUrlPathWs + "/@width", dom);
        assertXpathEvaluatesTo(String.valueOf(LEGEND_HEIGHT), legendUrlPathWs + "/@height", dom);
        assertXpathEvaluatesTo(LEGEND_FORMAT, legendUrlPathWs + "/wms:Format", dom);
        assertXpathEvaluatesTo(
                BASE + expectedGetLegendGraphicRequestURLWS,
                legendUrlPathWs + "/wms:OnlineResource/@xlink:href",
                dom);

        final String legendUrlPathHTTPLegend =
                "//wms:Layer[wms:Name='gs:" + HTTP_LEGEND_LAYER + "']/wms:Style/wms:LegendURL";

        // assert that legend resources are hidden behind a GetLegendGraphic request URL
        assertXpathEvaluatesTo(
                String.valueOf(LEGEND_WIDTH), legendUrlPathHTTPLegend + "/@width", dom);
        assertXpathEvaluatesTo(
                String.valueOf(LEGEND_HEIGHT), legendUrlPathHTTPLegend + "/@height", dom);
        assertXpathEvaluatesTo(LEGEND_FORMAT, legendUrlPathHTTPLegend + "/wms:Format", dom);
        assertXpathEvaluatesTo(
                BASE + expectedGetLegendGraphicRequestURLHttp,
                legendUrlPathHTTPLegend + "/wms:OnlineResource/@xlink:href",
                dom);
    }
}
