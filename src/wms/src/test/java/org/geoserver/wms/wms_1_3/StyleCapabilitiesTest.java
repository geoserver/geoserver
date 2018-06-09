/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.wms_1_1_1.CapabilitiesTest;
import org.geotools.styling.StyleImpl;
import org.junit.Test;
import org.w3c.dom.Document;

/** Tests for GEOS-8063: WMS1.3.0 SLD definition can break getcapabilities */
public class StyleCapabilitiesTest extends WMSTestSupport {
    private static final String CAPABILITIES_REQUEST = "wms?request=getCapabilities&version=1.3.0";

    private static final String LAYER_NAME_WITH_STYLE_TITLE = "states_with_style_title";
    private static final String LAYER_NAME_WITHOUT_STYLE_TITLE = "states_without_style_title";
    private static final String LAYER_NAME_WITHOUT_STYLE_DESCRIPTION =
            "states_without_style_description";

    private static final String STYLE_NAME_WITH_TITLE = "style_with_style_title";
    private static final String STYLE_NAME_WITHOUT_TITLE = "style_without_style_title";
    private static final String STYLE_NAME_WITHOUT_DESCRIPTION = "style_without_style_description";

    private static final QName LAYER_WITH_SYTLE_TITLE =
            new QName(MockData.DEFAULT_URI, LAYER_NAME_WITH_STYLE_TITLE, MockData.DEFAULT_PREFIX);
    private static final QName LAYER_WITHOUT_STYLE_TITLE =
            new QName(
                    MockData.DEFAULT_URI, LAYER_NAME_WITHOUT_STYLE_TITLE, MockData.DEFAULT_PREFIX);
    private static final QName LAYER_WITHOUT_STYLE_DESCRIPTION =
            new QName(
                    MockData.DEFAULT_URI,
                    LAYER_NAME_WITHOUT_STYLE_DESCRIPTION,
                    MockData.DEFAULT_PREFIX);

    private static final String BASE = "src/test/resources/geoserver";

    /**
     * Add 3 layers:
     *
     * <ul>
     *   <li>a layer with an sld with a title,
     *   <li>a layer with an sld without a title,
     *   <li>a layer with an sld without a description.
     * </ul>
     */
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();

        // add layers
        testData.addStyle(STYLE_NAME_WITH_TITLE, "styleWithTitle.sld", getClass(), catalog);
        testData.addStyle(STYLE_NAME_WITHOUT_TITLE, "styleWithoutTitle.sld", getClass(), catalog);
        testData.addStyle(
                STYLE_NAME_WITHOUT_DESCRIPTION, "styleWithoutDescription.sld", getClass(), catalog);

        Map<LayerProperty, Object> properties = new HashMap<LayerProperty, Object>();
        properties.put(LayerProperty.STYLE, STYLE_NAME_WITH_TITLE);

        testData.addVectorLayer(
                LAYER_WITH_SYTLE_TITLE,
                properties,
                "states.properties",
                CapabilitiesTest.class,
                catalog);

        properties = new HashMap<LayerProperty, Object>();
        properties.put(LayerProperty.STYLE, STYLE_NAME_WITHOUT_TITLE);

        testData.addVectorLayer(
                LAYER_WITHOUT_STYLE_TITLE,
                properties,
                "states.properties",
                CapabilitiesTest.class,
                catalog);

        properties = new HashMap<LayerProperty, Object>();
        properties.put(LayerProperty.STYLE, STYLE_NAME_WITHOUT_DESCRIPTION);

        testData.addVectorLayer(
                LAYER_WITHOUT_STYLE_DESCRIPTION,
                properties,
                "states.properties",
                CapabilitiesTest.class,
                catalog);

        // force the style without description to be null, by default it is not null if not set
        // https://github.com/geotools/geotools/blob/bdcdaeca35f0cb1c465f2e11dd1b04bb7fff30df/modules/library/main/src/main/java/org/geotools/styling/StyleImpl.java#L45
        StyleImpl style =
                (StyleImpl) catalog.getStyleByName(STYLE_NAME_WITHOUT_DESCRIPTION).getStyle();
        style.setDescription(null);

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
    public void testLayerStyleWithTitle() throws Exception {
        Document dom = dom(get(CAPABILITIES_REQUEST), false);
        // print(dom);
        // check we have the userStyle title
        assertXpathEvaluatesTo(
                "Population in the United States",
                getLayerStyleTitleXPath(LAYER_NAME_WITH_STYLE_TITLE),
                dom);
    }

    @Test
    public void testLayerStyleWithoutTitle() throws Exception {
        Document dom = dom(get(CAPABILITIES_REQUEST), false);
        // print(dom);
        // check we have the style name
        assertXpathEvaluatesTo(
                STYLE_NAME_WITHOUT_TITLE,
                getLayerStyleTitleXPath(LAYER_NAME_WITHOUT_STYLE_TITLE),
                dom);
    }

    @Test
    public void testLayerStyleWithoutDescription() throws Exception {
        Document dom = dom(get(CAPABILITIES_REQUEST), false);
        // print(dom);
        // check we have the style name
        assertXpathEvaluatesTo(
                STYLE_NAME_WITHOUT_DESCRIPTION,
                getLayerStyleTitleXPath(LAYER_NAME_WITHOUT_STYLE_DESCRIPTION),
                dom);
    }

    private String getLayerStyleTitleXPath(String layerName) {
        return "//wms:Layer[wms:Name='"
                + MockData.DEFAULT_PREFIX
                + ":"
                + layerName
                + "']/wms:Style/wms:Title";
    }
}
