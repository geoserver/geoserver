/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */ package org.geoserver.wms.capabilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
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
public class GetCapabilitiesScaleDenominatorTest extends WMSTestSupport {

    private final XpathEngine xpath;

    private static final String BASE_URL = "http://localhost/geoserver";

    /** Test layers */
    public static final QName REGIONATED =
            new QName(MockData.SF_URI, "Regionated", MockData.SF_PREFIX);

    public static final QName ACCIDENT = new QName(MockData.SF_URI, "Accident", MockData.SF_PREFIX);
    public static final QName ACCIDENT2 =
            new QName(MockData.SF_URI, "Accident2", MockData.SF_PREFIX);
    public static final QName ACCIDENT3 =
            new QName(MockData.SF_URI, "Accident3", MockData.SF_PREFIX);

    private Catalog catalog;

    public GetCapabilitiesScaleDenominatorTest() {

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("wms", "http://www.opengis.net/wms");
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
        layerGroup1.setMode(Mode.NAMED);

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
        checkWms13ValidationErrors(dom);

        Element layerElement = searchLayerElement("testLayerGroup1", dom);

        NodeList minScaleNode = layerElement.getElementsByTagName("MinScaleDenominator");
        Element minScaleElement = (Element) minScaleNode.item(0);

        NodeList maxScaleNode = layerElement.getElementsByTagName("MaxScaleDenominator");
        Element maxScaleElement = (Element) maxScaleNode.item(0);

        assertEquals(Double.valueOf(80000000), Double.valueOf(minScaleElement.getTextContent()));
        assertEquals(Double.valueOf(1000000000), Double.valueOf(maxScaleElement.getTextContent()));

        layerElement = searchLayerElement("testLayerGroup3", dom);

        minScaleNode = layerElement.getElementsByTagName("wms:MinScaleDenominator");
        minScaleElement = (Element) minScaleNode.item(0);

        maxScaleNode = layerElement.getElementsByTagName("wms:MaxScaleDenominator");
        maxScaleElement = (Element) minScaleNode.item(0);

        assertNull(minScaleElement);
        assertNull(maxScaleElement);
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

        Capabilities_1_3_0_Transformer tr =
                new Capabilities_1_3_0_Transformer(
                        wms,
                        BASE_URL,
                        wms.getAllowedMapFormats(),
                        new HashSet<ExtendedCapabilitiesProvider>());
        GetCapabilitiesRequest req = new GetCapabilitiesRequest();
        req.setBaseUrl(BASE_URL);
        req.setVersion(WMS.VERSION_1_3_0.toString());

        Document dom = WMSTestSupport.transform(req, tr);

        Element root = dom.getDocumentElement();
        assertEquals(WMS.VERSION_1_3_0.toString(), root.getAttribute("version"));

        return dom;
    }

    /**
     * Searches the required layer in the capabilities document.
     *
     * @return The layer element or null it the required layer isn't found
     */
    private Element searchLayerElement(final String layerRequired, Document capabilities)
            throws XpathException {

        NodeList layersNodes = xpath.getMatchingNodes("//wms:Layer/wms:Name", capabilities);
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
