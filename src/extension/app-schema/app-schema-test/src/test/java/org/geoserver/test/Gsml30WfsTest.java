/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.geotools.wfs.v2_0.WFS;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS test based on GeoSciML 3.0rc1, a GML 3.2 application schema.
 *
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class Gsml30WfsTest extends AbstractAppSchemaTestSupport {

    /** @see org.geoserver.test.AbstractAppSchemaTestSupport#buildTestData() */
    @Override
    protected Gsml30MockData createTestData() {
        return new Gsml30MockData();
    }

    /** Test DescribeFeatureType response. */
    @Test
    public void testDescribeFeatureType() {
        String path = "wfs?request=DescribeFeatureType&typename=gsml:MappedFeature&version=1.1.0";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());
        // check target name space is encoded and is correct
        assertXpathEvaluatesTo(getNamespace("gsml"), "//@targetNamespace", doc);
        // make sure the content is only relevant include
        assertXpathCount(1, "//xsd:include", doc);
        // no import to gml since it is already imported inside the included schema
        assertXpathCount(0, "//xsd:import", doc);
        // gsml schemaLocation
        assertXpathEvaluatesTo(
                Gsml30MockData.GSML_SCHEMA_LOCATION, "//xsd:include/@schemaLocation", doc);
        // nothing else
        assertXpathCount(0, "//xsd:complexType", doc);
        assertXpathCount(0, "//xsd:element", doc);
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() {
        String path = "wfs?request=GetFeature&typename=gsml:MappedFeature&outputFormat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        assertEquals(WFS.NAMESPACE, doc.getDocumentElement().getNamespaceURI());
        assertEquals("FeatureCollection", doc.getDocumentElement().getLocalName());
    }

    /** Test whether GetFeature response is schema-valid. */
    @Test
    public void testGetFeatureValid() {
        String path = "wfs?request=GetFeature&typename=gsml:MappedFeature&outputFormat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        validateGet(path);
    }

    /** Test content of GetFeature response. */
    @Test
    public void testGetFeatureContent() throws Exception {
        String path = "wfs?request=GetFeature&typename=gsml:MappedFeature&outputFormat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathEvaluatesTo("unknown", "/wfs:FeatureCollection/@numberMatched", doc);
        assertXpathCount(2, "//gsml:MappedFeature", doc);
        // test names
        assertXpathEvaluatesTo("First", "//gsml:MappedFeature[@gml:id='mf.1']/gml:name", doc);
        assertXpathEvaluatesTo("Second", "//gsml:MappedFeature[@gml:id='mf.2']/gml:name", doc);
        assertXpathEvaluatesTo(
                "250000",
                "//gsml:MappedFeature[@gml:id='mf.1']/gsml:resolutionScale"
                        + "/gmd:MD_RepresentativeFraction/gmd:denominator/gco:Integer",
                doc);
        assertXpathEvaluatesTo(
                "250000",
                "//gsml:MappedFeature[@gml:id='mf.2']/gsml:resolutionScale"
                        + "/gmd:MD_RepresentativeFraction/gmd:denominator/gco:Integer",
                doc);
    }

    /** Test namespace of GetFeature response. */
    @Test
    public void testNamespace() throws Exception {
        String path = "wfs?request=GetFeature&typename=gsml:MappedFeature&outputFormat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathEvaluatesTo("unknown", "/wfs:FeatureCollection/@numberMatched", doc);
        assertXpathCount(2, "/wfs:FeatureCollection/wfs:member", doc);
        // test that all namespaces are present on the root element
        for (String prefix : getRequiredNamespaces().keySet()) {
            assertEquals(
                    getNamespace(prefix),
                    doc.getFirstChild()
                            .getAttributes()
                            .getNamedItemNS(XMLNS, prefix)
                            .getTextContent());
        }
        // test that no namespaces are present on the wfs:member elements
        assertEquals(0, doc.getFirstChild().getChildNodes().item(0).getAttributes().getLength());
        assertEquals(0, doc.getFirstChild().getChildNodes().item(1).getAttributes().getLength());
    }

    private Map<String, String> getRequiredNamespaces() {
        final Map<String, String> requiredNamespaces = new HashMap<>();
        requiredNamespaces.put("gsml", "urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0");
        requiredNamespaces.put("gmd", "http://www.isotc211.org/2005/gmd");
        requiredNamespaces.put("gco", "http://www.isotc211.org/2005/gco");
        requiredNamespaces.put("cgu", "urn:cgi:xmlns:CGI:Utilities:3.0.0");
        requiredNamespaces.put("swe", "http://www.opengis.net/swe/1.0/gml32");
        return requiredNamespaces;
    }
}
