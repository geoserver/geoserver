/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LayerWorkspaceTest extends WMSTestSupport {

    private Catalog catalog;

    @Before
    public void setCatalog() throws Exception {
        catalog = getCatalog();
    }

    LayerInfo layer(Catalog cat, QName name) {
        return cat.getLayerByName(getLayerId(name));
    }

    /** Test layer names order from GetCapabilities */
    @Test
    public void testLayerOrderGetCapabilities() throws Exception {
        Document doc = getAsDOM("/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        List<String> originalList = layerNameList(doc);
        assertFalse(originalList.isEmpty());
        List<String> names =
                originalList.stream().map(x -> removeLayerPrefix(x)).collect(Collectors.toList());
        List<String> orderedNames = names.stream().sorted().collect(Collectors.toList());
        assertTrue(orderedNames.equals(names));
    }

    /** Test layer names order from GetCapabilities on workspace */
    @Test
    public void testWorkspaceLayerOrderGetCapabilities() throws Exception {
        Document doc =
                getAsDOM("/cite/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        List<String> originalList = layerNameList(doc);
        assertFalse(originalList.isEmpty());
        assertTrue(originalList.stream().noneMatch(x -> x.indexOf(":") > -1));
        List<String> orderedNames = originalList.stream().sorted().collect(Collectors.toList());
        assertTrue(orderedNames.equals(originalList));
    }

    /** removes prefix from layer name */
    private String removeLayerPrefix(String prefixedName) {
        if (prefixedName.indexOf(":") > -1) {
            return prefixedName.split(":")[1];
        }
        return prefixedName;
    }

    /** returns list of prefixed layer names from document */
    private List<String> layerNameList(Document doc) throws Exception {
        List<Node> nlist = xpathList("//WMT_MS_Capabilities/Capability/Layer/Layer/Name", doc);
        List<String> result = new ArrayList<>();
        nlist.forEach(
                x -> {
                    result.add(x.getTextContent().trim());
                });
        return result;
    }

    private List<Node> xpathList(String xpathString, Document doc) throws XPathExpressionException {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile(xpathString);
        NodeList nlist = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        List<Node> nodeList = new ArrayList<>();
        for (int i = 0; i < nlist.getLength(); i++) {
            nodeList.add(nlist.item(i));
        }
        return nodeList;
    }

    @Test
    public void testGlobalCapabilities() throws Exception {
        LayerInfo layer = layer(catalog, MockData.PRIMITIVEGEOFEATURE);
        Document doc = getAsDOM("/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathExists("//Layer[Name='" + layer.prefixedName() + "']", doc);
    }

    @Test
    public void testGlobalDescribeLayer() throws Exception {
        LayerInfo layer = layer(catalog, MockData.PRIMITIVEGEOFEATURE);
        Document doc =
                getAsDOM(
                        "/wms?service=WMS&request=describeLayer&version=1.1.1&LAYERS="
                                + layer.getName(),
                        true);
        assertXpathExists("//LayerDescription[@name='" + layer.prefixedName() + "']", doc);
    }

    @Test
    public void testWorkspaceCapabilities() throws Exception {
        Document doc = getAsDOM("/sf/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathExists(
                "//Layer[Name='" + MockData.PRIMITIVEGEOFEATURE.getLocalPart() + "']", doc);
    }

    @Test
    public void testWorkspaceDescribeLayer() throws Exception {
        Document doc =
                getAsDOM(
                        "/sf/wms?service=WMS&request=describeLayer&version=1.1.1&LAYERS="
                                + MockData.PRIMITIVEGEOFEATURE.getLocalPart(),
                        true);
        assertXpathExists(
                "//LayerDescription[@name='" + MockData.PRIMITIVEGEOFEATURE.getLocalPart() + "']",
                doc);
    }
}
