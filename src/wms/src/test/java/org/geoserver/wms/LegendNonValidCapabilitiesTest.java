/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.platform.resource.Resource;
import org.junit.Test;
import org.w3c.dom.Document;

/** Test setup for GEOS-8982. Does a check on Style capabilities using a non valid LegendInfo. */
public class LegendNonValidCapabilitiesTest extends WMSTestSupport {

    private static final String CAPABILITIES_REQUEST_1_3_0 =
            "wms?request=getCapabilities&version=1.3.0";
    private static final String CAPABILITIES_REQUEST_1_1_1 =
            "wms?request=getCapabilities&version=1.1.1";

    private static final String LAYER_NAME = "watertemp";
    private static final QName LAYER_QNAME =
            new QName(MockData.DEFAULT_URI, LAYER_NAME, MockData.DEFAULT_PREFIX);
    private static final String LAYER_FILE = "custwatertemp.zip";
    private static final String STYLE_NAME = "temperature";
    private static final String STYLE_FILE = "temperature.sld";
    private static final String LEGEND_FORMAT = "image/png";

    private static final int LEGEND_WIDTH = 22;
    private static final int LEGEND_HEIGHT = 121;
    private static final String BASE = "http://127.0.0.1:8080/geoserver";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // create the non-valid legendInfo to test
        LegendInfo legend = new LegendInfoImpl();
        legend.setWidth(0);
        legend.setHeight(0);
        legend.setFormat(LEGEND_FORMAT);
        legend.setOnlineResource(null);

        // add legend.png to styles directory
        Resource resource = getResourceLoader().get("styles/legend.png");
        getResourceLoader().copyFromClassPath("legend.png", resource.file(), getClass());

        // add layer
        testData.addStyle(null, STYLE_NAME, STYLE_FILE, getClass(), getCatalog(), legend);
        Map<SystemTestData.LayerProperty, Object> propertyMap =
                new HashMap<SystemTestData.LayerProperty, Object>();
        propertyMap.put(LayerProperty.STYLE, STYLE_NAME);

        testData.addRasterLayer(
                LAYER_QNAME, LAYER_FILE, null, propertyMap, SystemTestData.class, getCatalog());

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

    @Test
    public void testCapabilities_1_3_0() throws Exception {
        Document dom = dom(get(CAPABILITIES_REQUEST_1_3_0), false);

        final String legendUrlPath =
                "//wms:Layer[wms:Name='gs:" + LAYER_NAME + "']/wms:Style/wms:LegendURL";

        // Ensure capabilities document reflects the specified legend info
        assertXpathEvaluatesTo(String.valueOf(LEGEND_WIDTH), legendUrlPath + "/@width", dom);
        assertXpathEvaluatesTo(String.valueOf(LEGEND_HEIGHT), legendUrlPath + "/@height", dom);
        assertXpathEvaluatesTo(LEGEND_FORMAT, legendUrlPath + "/wms:Format", dom);
        assertXpathEvaluatesTo(
                BASE
                        + "/ows?service=WMS&request=GetLegendGraphic&format=image%2Fpng&width=20"
                        + "&height=20&layer=gs%3Awatertemp",
                legendUrlPath + "/wms:OnlineResource/@xlink:href",
                dom);
    }

    @Test
    public void testCapabilities_1_1_1() throws Exception {
        Document dom = dom(get(CAPABILITIES_REQUEST_1_1_1), true);
        final String legendUrlPath = "//Layer[Name='gs:" + LAYER_NAME + "']/Style/LegendURL";

        // Ensure capabilities document reflects the specified legend info
        assertXpathEvaluatesTo(String.valueOf(LEGEND_WIDTH), legendUrlPath + "/@width", dom);
        assertXpathEvaluatesTo(String.valueOf(LEGEND_HEIGHT), legendUrlPath + "/@height", dom);
        assertXpathEvaluatesTo(LEGEND_FORMAT, legendUrlPath + "/Format", dom);
        assertXpathEvaluatesTo(
                BASE
                        + "/wms?request=GetLegendGraphic&format=image%2Fpng&width=20"
                        + "&height=20&layer=gs%3Awatertemp",
                legendUrlPath + "/OnlineResource/@xlink:href",
                dom);
    }
}
