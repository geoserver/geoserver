/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.geotools.appschema.resolver.data.SampleDataAccess;
import org.geotools.appschema.resolver.data.SampleDataAccessData;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * WFS GetFeature to test integration of {@link SampleDataAccess} with GeoServer.
 *
 * @author Ben Caradoc-Davies, CSIRO Exploration and Mining
 */
public class SampleDataAccessWfsTest extends SampleDataAccessTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // Setup XMLUnit namespaces
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put(SampleDataAccessData.NAMESPACE_PREFIX, SampleDataAccessData.NAMESPACE_URI);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {}

    /** Test whether GetCapabilities returns wfs:WFS_Capabilities. */
    @Test
    public void testGetCapabilities() throws Exception {
        Document doc = getAsDOM("wfs?request=GetCapabilities&version=1.1.0");
        LOGGER.info("WFS GetCapabilities response:\n" + prettyString(doc));
        assertEquals("wfs:WFS_Capabilities", doc.getDocumentElement().getNodeName());
    }

    /** Test whether DescribeFeatureType returns xsd:schema. */
    @Test
    public void testDescribeFeatureType() throws Exception {
        Document doc =
                getAsDOM(
                        "wfs?request=DescribeFeatureType&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS DescribeFeatureType response:\n" + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() throws Exception {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
    }

    /** Test content of GetFeature response. */
    @Test
    public void testGetFeatureContent() throws Exception {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");

        assertXpathCount(2, "//gsml:MappedFeature", doc);

        // mf1
        XMLAssert.assertXpathEvaluatesTo(
                "GUNTHORPE FORMATION", "//gsml:MappedFeature[@gml:id='mf1']/gml:description", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "mf1.spec",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification"
                        + "/gsml:GeologicUnit/@gml:id",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "Gunthorpe specification description",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:description",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "-1.2 52.5 -1.2 52.6 -1.1 52.6 -1.1 52.5 -1.2 52.5",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:shape//gml:posList",
                doc);

        // mf2
        XMLAssert.assertXpathEvaluatesTo(
                "MERCIA MUDSTONE GROUP",
                "//gsml:MappedFeature[@gml:id='mf2']/gml:description",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "mf2.spec",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification"
                        + "/gsml:GeologicUnit/@gml:id",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "Mercia specification description",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:description",
                doc);
        XMLAssert.assertXpathEvaluatesTo(
                "-1.3 52.5 -1.3 52.6 -1.2 52.6 -1.2 52.5 -1.3 52.5",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:shape//gml:posList",
                doc);
    }

    /**
     * Assert that there are count matches of xpath in doc.
     *
     * @param count expected number of matches
     * @param xpath xpath expression
     * @param doc document under test
     */
    public void assertXpathCount(int count, String xpath, Document doc) throws Exception {
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList nodes = engine.getMatchingNodes(xpath, doc);
        assertEquals(count, nodes.getLength());
    }

    /** Return {@link Document} as a pretty-printed string. */
    public String prettyString(Document doc) throws Exception {
        OutputStream out = new ByteArrayOutputStream();
        prettyPrint(doc, out);
        return out.toString();
    }

    /** Pretty-print a {@link Document} to an {@link OutputStream}. */
    public void prettyPrint(Document doc, OutputStream out) throws Exception {
        Transformer tx = TransformerFactory.newInstance().newTransformer();
        tx.setOutputProperty(OutputKeys.INDENT, "yes");
        tx.transform(new DOMSource(doc), new StreamResult(out));
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpSecurity();
    }
}
