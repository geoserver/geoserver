/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */ package org.geoserver.wms.capabilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Test cases for Capabilities' ScaleHint
 *
 * @author Mauricio Pazos
 * @author Niels Charlier
 */
public class GetCapabilitiesScaleHintTest extends WMSTestSupport {

    private final XpathEngine xpath;

    private static final String BASE_URL = "http://localhost/geoserver";

    private static final Set<String> FORMATS = Collections.singleton("image/png");

    private static final Set<String> LEGEND_FORMAT = Collections.singleton("image/png");

    /** Test layers */
    public static final QName REGIONATED =
            new QName(MockData.SF_URI, "Regionated", MockData.SF_PREFIX);

    public static final QName ACCIDENT = new QName(MockData.SF_URI, "Accident", MockData.SF_PREFIX);
    public static final QName ACCIDENT2 =
            new QName(MockData.SF_URI, "Accident2", MockData.SF_PREFIX);
    public static final QName ACCIDENT3 =
            new QName(MockData.SF_URI, "Accident3", MockData.SF_PREFIX);

    private Catalog catalog;

    public GetCapabilitiesScaleHintTest() {

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // all the data we need is registered in this test
        testData.setUpSecurity();
    }

    /**
     * Adds required styles to test the selection of maximum and minimum denominator from style's
     * rules.
     */
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {

        this.catalog = getCatalog();

        addLayerAndStyle(testData, REGIONATED);
        addLayerAndStyle(testData, ACCIDENT);
        addLayerAndStyle(testData, ACCIDENT2);
        addLayerAndStyle(testData, ACCIDENT3);
        addLayerGroups(testData);
    }

    void addLayerAndStyle(SystemTestData testData, QName name) throws IOException {
        testData.addVectorLayer(
                name, null, name.getLocalPart() + ".properties", getClass(), this.catalog);

        final String styleName = name.getLocalPart();
        testData.addStyle(styleName, getClass(), this.catalog);

        StyleInfo defaultStyle = this.catalog.getStyleByName(styleName);

        String layerId = getLayerId(name);
        LayerInfo layerInfo = this.catalog.getLayerByName(layerId);
        layerInfo.setDefaultStyle(defaultStyle);
        this.catalog.save(layerInfo);
    }

    void addLayerGroups(SystemTestData testData) throws Exception {
        // setup basic layergroups
        testData.addStyle("Accident3_2", getClass(), this.catalog);

        CoordinateReferenceSystem nativeCrs = CRS.decode("EPSG:4326", true);
        ReferencedEnvelope nativeBounds = new ReferencedEnvelope(-180, 180, -90, 90, nativeCrs);

        LayerGroupInfo layerGroup1 = catalog.getFactory().createLayerGroup();
        layerGroup1.setName("testLayerGroup1");
        layerGroup1.setBounds(nativeBounds);

        LayerGroupInfo layerGroup2 = catalog.getFactory().createLayerGroup();
        layerGroup2.setName("testLayerGroup2");
        layerGroup2.setBounds(nativeBounds);

        LayerGroupInfo layerGroup3 = catalog.getFactory().createLayerGroup();
        layerGroup3.setName("testLayerGroup3");
        layerGroup3.setBounds(nativeBounds);

        // add layers & styles
        layerGroup1.getLayers().add(catalog.getLayerByName(getLayerId(REGIONATED)));
        layerGroup1.getStyles().add(null);
        layerGroup1.getLayers().add(catalog.getLayerByName(getLayerId(ACCIDENT3)));
        layerGroup1.getStyles().add(catalog.getStyleByName("Accident3_2"));

        layerGroup2.getLayers().add(catalog.getLayerByName(getLayerId(REGIONATED)));
        layerGroup2.getLayers().add(catalog.getLayerByName(getLayerId(ACCIDENT)));
        layerGroup2.getLayers().add(catalog.getLayerByName(getLayerId(ACCIDENT2)));
        layerGroup2.getStyles().add(null);
        layerGroup2.getStyles().add(null);
        layerGroup2.getStyles().add(null);

        layerGroup3.getLayers().add(layerGroup2);
        layerGroup3.getLayers().add(catalog.getLayerByName(getLayerId(ACCIDENT3)));
        layerGroup3.getStyles().add(null);
        layerGroup3.getStyles().add(null);

        catalog.add(layerGroup1);
        catalog.add(layerGroup2);
        catalog.add(layerGroup3);
    }

    @Test
    public void testLayerGroups() throws Exception {

        Document dom = findCapabilities(false);

        // print(dom);

        Element layerElement = searchLayerElement("testLayerGroup1", dom);

        NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");
        Element scaleElement = (Element) scaleNode.item(0);

        assertEquals(Double.valueOf(80000000), Double.valueOf(scaleElement.getAttribute("min")));
        assertEquals(Double.valueOf(1000000000), Double.valueOf(scaleElement.getAttribute("max")));

        layerElement = searchLayerElement("testLayerGroup3", dom);
        scaleNode = layerElement.getElementsByTagName("ScaleHint");
        scaleElement = (Element) scaleNode.item(0);

        assertNull(scaleElement);
    }

    /**
     * Default values for ScaleHint should be set.
     *
     * <pre>
     * The computation of Min and Max values return:
     * 		Min: 0.0
     * 		Max: infinity
     *
     * Capabilities document Expected:
     *
     * 		ScaleHint element shouldn't be generated.
     * </pre>
     */
    @Test
    public void scaleHintDefaultValues() throws Exception {

        Document dom = findCapabilities(false);

        Element layerElement = searchLayerElement(getLayerId(ACCIDENT), dom);

        NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");

        Element scaleElement = (Element) scaleNode.item(0);

        assertTrue(scaleElement == null); // scale hint is not generated
    }

    /**
     * Default values for ScaleHint should be set.
     *
     * <pre>
     * Check the min and max values return:
     * 		Min: 0.0
     * 		Max: a value
     *
     * Capabilities document Expected:
     *
     *   <ScaleHint min=0 max=value/>
     * </pre>
     */
    @Test
    public void scaleHintDefaultMinValue() throws Exception {

        Document dom = findCapabilities(false);

        Element layerElement = searchLayerElement(getLayerId(ACCIDENT2), dom);

        NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");

        Element scaleElement = (Element) scaleNode.item(0);

        assertEquals(0.0, Double.valueOf(scaleElement.getAttribute("min")), 0d);

        assertEquals(Double.valueOf(640000000), Double.valueOf(scaleElement.getAttribute("max")));
    }

    /**
     * Default values for ScaleHint should be set.
     *
     * <pre>
     * The computation of Min and Max values when the option
     * 'Scalehint in units per diagonal pixel' is set. Return:
     * 		Min: 0.0
     * 		Max: a value
     *
     * Capabilities document Expected:
     *
     *   <ScaleHint min=0 max=value/>
     * </pre>
     */
    @Test
    public void scaleHintUnitsPerPixelDefaultMinValue() throws Exception {

        Document dom = findCapabilities(true);

        Element layerElement = searchLayerElement(getLayerId(ACCIDENT2), dom);

        NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");

        Element scaleElement = (Element) scaleNode.item(0);

        assertEquals(0.0, Double.valueOf(scaleElement.getAttribute("min")), 0d);

        assertEquals(
                Double.valueOf(253427.07037725858),
                Double.valueOf(scaleElement.getAttribute("max")));
    }

    /**
     * Default values for ScaleHint should be set.
     *
     * <pre>
     * Check the Min and Max values return:
     * 		Min: a value
     * 		Max: Infinity
     *
     * Capabilities document Expected:
     *
     *   <ScaleHint min=value max=infinity/>
     * </pre>
     */
    @Test
    public void scaleHintDefaultMaxValue() throws Exception {

        Document dom = findCapabilities(false);

        Element layerElement = searchLayerElement(getLayerId(ACCIDENT3), dom);

        NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");

        Element scaleElement = (Element) scaleNode.item(0);

        assertEquals(Double.valueOf(320000000), Double.valueOf(scaleElement.getAttribute("min")));
        assertEquals(
                Double.POSITIVE_INFINITY, Double.valueOf(scaleElement.getAttribute("max")), 0d);
    }

    /**
     * Default values for ScaleHint should be set.
     *
     * <pre>
     * The computation of Min and Max values when the option
     * 'Scalehint in units per diagonal pixel' is set. Return:
     * 		Min: a value
     * 		Max: Infinity
     *
     * Capabilities document Expected:
     *
     *   <ScaleHint min=value max=infinity/>
     * </pre>
     */
    @Test
    public void scaleHintUnitsPerPixelDefaultMaxValue() throws Exception {

        Document dom = findCapabilities(true);

        Element layerElement = searchLayerElement(getLayerId(ACCIDENT3), dom);

        NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");

        Element scaleElement = (Element) scaleNode.item(0);

        assertEquals(
                Double.valueOf(126713.53518862929),
                Double.valueOf(scaleElement.getAttribute("min")));
        assertEquals(
                Double.POSITIVE_INFINITY, Double.valueOf(scaleElement.getAttribute("max")), 0d);
    }

    /**
     *
     *
     * <pre>
     * Max is the maximum value found in the set of rules
     * Min is the minimum value found in the set of rules
     * </pre>
     */
    @Test
    public void scaleHintFoundMaxMinDenominators() throws Exception {

        Document dom = findCapabilities(false);

        final String layerName = getLayerId(REGIONATED);
        Element layerElement = searchLayerElement(layerName, dom);

        NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");
        Element scaleElement = (Element) scaleNode.item(0);

        assertEquals(Double.valueOf(80000000), Double.valueOf(scaleElement.getAttribute("min")));
        assertEquals(Double.valueOf(640000000), Double.valueOf(scaleElement.getAttribute("max")));
    }

    /**
     *
     *
     * <pre>
     * Max is the maximum value found in the set of rules
     * Min is the minimum value found in the set of rules
     * Both values are computed as units per diagonal pixel
     * </pre>
     */
    @Test
    public void scaleHintUnitsPerPixelFoundMaxMinDenominators() throws Exception {

        Document dom = findCapabilities(true);

        final String layerName = getLayerId(REGIONATED);
        Element layerElement = searchLayerElement(layerName, dom);

        NodeList scaleNode = layerElement.getElementsByTagName("ScaleHint");
        Element scaleElement = (Element) scaleNode.item(0);

        assertEquals(
                Double.valueOf(31678.383797157323),
                Double.valueOf(scaleElement.getAttribute("min")));
        assertEquals(
                Double.valueOf(253427.07037725858),
                Double.valueOf(scaleElement.getAttribute("max")));
    }
    /**
     * Retrieves the WMS's capabilities document.
     *
     * @param scaleHintUnitsPerDiaPixel true if the scalehint must be in units per diagonal of a
     *     pixel
     * @return Capabilities as {@link Document}
     */
    private Document findCapabilities(Boolean scaleHintUnitsPerDiaPixel) throws Exception {
        // set the Scalehint units per diagonal pixel setting.
        WMS wms = getWMS();
        WMSInfo info = wms.getServiceInfo();
        MetadataMap mm = info.getMetadata();
        mm.put(WMS.SCALEHINT_MAPUNITS_PIXEL, scaleHintUnitsPerDiaPixel);
        info.getGeoServer().save(info);

        GetCapabilitiesTransformer tr =
                new GetCapabilitiesTransformer(wms, BASE_URL, FORMATS, LEGEND_FORMAT, null);
        GetCapabilitiesRequest req = new GetCapabilitiesRequest();
        req.setBaseUrl(BASE_URL);
        req.setVersion(WMS.VERSION_1_1_1.toString());

        Document dom = WMSTestSupport.transform(req, tr);

        Element root = dom.getDocumentElement();
        Assert.assertEquals(WMS.VERSION_1_1_1.toString(), root.getAttribute("version"));

        return dom;
    }

    /**
     * Searches the required layer in the capabilities document.
     *
     * @return The layer element or null it the required layer isn't found
     */
    private Element searchLayerElement(final String layerRequired, Document capabilities)
            throws XpathException {

        NodeList layersNodes = xpath.getMatchingNodes("//Layer/Name", capabilities);
        for (int i = 0; i < layersNodes.getLength(); i++) {

            Element e = (Element) layersNodes.item(i);
            NodeList childNodes = e.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {

                Node item = childNodes.item(j);
                String nodeValue = item.getNodeValue();

                if (layerRequired.equalsIgnoreCase(nodeValue)) {

                    return (Element)
                            e.getParentNode(); // returns the layer element associated to the
                    // required layer name.
                }
            }
        }
        return null; // not found
    }
}
