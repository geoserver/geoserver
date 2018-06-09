/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests for a multivalued xlink:href ClientProperty mapping without feature chaining.
 *
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
public class MultivaluedXlinkHrefTest extends AbstractAppSchemaTestSupport {

    @Override
    protected MultivaluedXlinkHrefMockData createTestData() {
        return new MultivaluedXlinkHrefMockData();
    }

    /**
     * Test that GetFeature returns a single feature with two gsml:occurrence, each with expected
     * xlink:href.
     */
    @Test
    public void testGetFeature() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=2.0.0&request=GetFeature&typenames=gsml:GeologicUnit");
        LOGGER.info("WFS GetFeature, typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.1", "//gsml:GeologicUnit/@gml:id", doc);
        assertXpathCount(2, "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence", doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.2",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.3",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[2]/@xlink:href",
                doc);
    }

    /**
     * Test that GetFeature filter on first gsml:occurrence/@xlink:href returns a single feature
     * with two gsml:occurrence, each with expected xlink:href.
     */
    @Test
    public void testGetFeatureFilterFirstXlinkHref() throws Exception {
        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"2.0.0\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + ">" //
                        + "    <wfs:Query typeNames=\"gsml:GeologicUnit\">" //
                        + "        <fes:Filter>" //
                        + "            <fes:PropertyIsEqualTo>" //
                        + "                <fes:ValueReference>gsml:occurrence/@xlink:href</fes:ValueReference>" //
                        + "                <fes:Literal>http://resource.example.org/mapped-feature/mf.2</fes:Literal>" //
                        + "            </fes:PropertyIsEqualTo>" //
                        + "        </fes:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info(prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.1", "//gsml:GeologicUnit/@gml:id", doc);
        assertXpathCount(2, "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence", doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.2",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.3",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[2]/@xlink:href",
                doc);
    }

    /**
     * Test that GetFeature filter on second gsml:occurrence/@xlink:href returns a single feature
     * with two gsml:occurrence, each with expected xlink:href.
     */
    @Test
    public void testGetFeatureFilterSecondXlinkHref() throws Exception {
        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"2.0.0\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + ">" //
                        + "    <wfs:Query typeNames=\"gsml:GeologicUnit\">" //
                        + "        <fes:Filter>" //
                        + "            <fes:PropertyIsEqualTo>" //
                        + "                <fes:ValueReference>gsml:occurrence/@xlink:href</fes:ValueReference>" //
                        + "                <fes:Literal>http://resource.example.org/mapped-feature/mf.3</fes:Literal>" //
                        + "            </fes:PropertyIsEqualTo>" //
                        + "        </fes:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info(prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.1", "//gsml:GeologicUnit/@gml:id", doc);
        assertXpathCount(2, "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence", doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.2",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "http://resource.example.org/mapped-feature/mf.3",
                "//gsml:GeologicUnit[@gml:id='gu.1']/gsml:occurrence[2]/@xlink:href",
                doc);
    }

    /**
     * Test that GetFeature filter on nonexistent gsml:occurrence/@xlink:href returns no features.
     */
    @Test
    public void testGetFeatureFilterNonexistentXlinkHref() throws Exception {
        String xml = //
                "<wfs:GetFeature " //
                        + "service=\"WFS\" " //
                        + "version=\"2.0.0\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + ">" //
                        + "    <wfs:Query typeNames=\"gsml:GeologicUnit\">" //
                        + "        <fes:Filter>" //
                        + "            <fes:PropertyIsEqualTo>" //
                        + "                <fes:ValueReference>gsml:occurrence/@xlink:href</fes:ValueReference>" //
                        + "                <fes:Literal>http://resource.example.org/mapped-feature/does-not-exist</fes:Literal>" //
                        + "            </fes:PropertyIsEqualTo>" //
                        + "        </fes:Filter>" //
                        + "    </wfs:Query> " //
                        + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info(prettyString(doc));
        assertXpathCount(0, "//gsml:GeologicUnit", doc);
    }
}
