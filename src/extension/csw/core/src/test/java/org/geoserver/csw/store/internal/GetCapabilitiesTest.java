/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geotools.filter.v1_1.OGC;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GetCapabilitiesTest extends CSWInternalTestSupport {

    static XpathEngine xpath = XMLUnit.newXpathEngine();

    static {
        Map<String, String> prefixMap = new HashMap<String, String>();
        prefixMap.put("ows", OWS.NAMESPACE);
        prefixMap.put("ogc", OGC.NAMESPACE);
        NamespaceContext nameSpaceContext = new SimpleNamespaceContext(prefixMap);
        xpath.setNamespaceContext(nameSpaceContext);
    }

    @Test
    public void testGetBasic() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetCapabilities");
        // print(dom);
        checkValidationErrors(dom);

        // basic check on local name
        Element e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());

        // basic check on xpath node
        assertXpathEvaluatesTo("1", "count(/csw:Capabilities)", dom);

        assertTrue(
                xpath.getMatchingNodes("//ows:OperationsMetadata/ows:Operation", dom).getLength()
                        > 0);
        assertEquals("5", xpath.evaluate("count(//ows:Operation)", dom));

        // basic check on GetCapabilities operation constraint
        assertEquals(
                "XML",
                xpath.evaluate(
                        "//ows:OperationsMetadata/ows:Operation[@name=\"GetCapabilities\"]/ows:Constraint/ows:Value",
                        dom));

        // check we have csw:AnyText among the queriables
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:Operation[@name='GetRecords']/ows:Constraint[@name='SupportedDublinCoreQueryables' and ows:Value = 'csw:AnyText'])",
                dom);

        // check we have dc:subject among the domain property names
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:Operation[@name='GetDomain']/ows:Parameter[@name='PropertyName' and ows:Value = 'dc:title'])",
                dom);
    }

    @Test
    public void testPostBasic() throws Exception {
        Document dom = postAsDOM(BASEPATH + "?service=csw&version=2.0.2&request=GetCapabilities");
        // print(dom);
        checkValidationErrors(dom);

        // basic check on local name
        Element e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());

        // basic check on xpath node
        assertXpathEvaluatesTo("1", "count(/csw:Capabilities)", dom);

        assertTrue(
                xpath.getMatchingNodes("//ows:OperationsMetadata/ows:Operation", dom).getLength()
                        > 0);
        assertEquals("5", xpath.evaluate("count(//ows:Operation)", dom));

        // basic check on GetCapabilities operation constraint
        assertEquals(
                "XML",
                xpath.evaluate(
                        "//ows:OperationsMetadata/ows:Operation[@name=\"GetCapabilities\"]/ows:Constraint/ows:Value",
                        dom));
    }

    @Test
    public void testSections() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?service=csw&version=2.0.2&request=GetCapabilities&sections=ServiceIdentification,ServiceProvider");
        // print(dom);
        checkValidationErrors(dom);

        // basic check on local name
        Element e = dom.getDocumentElement();
        assertEquals("Capabilities", e.getLocalName());

        // basic check on xpath node
        assertXpathEvaluatesTo("1", "count(/csw:Capabilities)", dom);
        assertEquals("1", xpath.evaluate("count(//ows:ServiceIdentification)", dom));
        assertEquals("1", xpath.evaluate("count(//ows:ServiceProvider)", dom));
        assertEquals("0", xpath.evaluate("count(//ows:OperationsMetadata)", dom));
        // this one is mandatory, cannot be skipped
        assertEquals("1", xpath.evaluate("count(//ogc:Filter_Capabilities)", dom));

        assertTrue(
                xpath.getMatchingNodes("//ows:OperationsMetadata/ows:Operation", dom).getLength()
                        == 0);
        assertEquals("0", xpath.evaluate("count(//ows:Operation)", dom));
    }
}
