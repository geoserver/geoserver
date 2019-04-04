/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.junit.Test;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Test paging with app-schema.
 *
 * @author Rini Angreani, CSIRO Mineral Resources Flagship
 */
public class PagingTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32MockData createTestData() {
        FeatureGML32MockData mockData = new FeatureGML32MockData();
        mockData.addStyle("namefilter", "styles/mappedfeaturebyname.sld");
        return mockData;
    }

    private void checkMf1Values(Document doc) {
        assertXpathEvaluatesTo(
                "GUNTHORPE FORMATION", "//gsml:MappedFeature[@gml:id='mf1']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:crs:EPSG::4326",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:CGI:EarthNaturalSurface",
                "//gsml:MappedFeature[@gml:id=\"mf1\"]/gsml:samplingFrame/@xlink:href",
                doc);
        // specification gu.25699
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification/gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo(
                "gu.25699",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        // description
        assertXpathEvaluatesTo(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:description",
                doc);
        // name
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "Yaugher Volcanic Group",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name[@codeSpace='urn:ietf:rfc:2141']",
                doc);
        // multi-valued leaf attributes that are feature chained come in random order
        // when joining is used
        List<String> names = new ArrayList<String>();
        names.add("Yaugher Volcanic Group");
        names.add("-Py");
        String name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification/gsml:GeologicUnit/gml:name[1]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification/gsml:GeologicUnit/gml:name[2]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

        assertXpathEvaluatesTo(
                "instance",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:purpose",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::unknown",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:geologicUnitType/@xlink:href",
                doc);
        // occurrence
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf1",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence/@xlink:href",
                doc);
    }

    private void checkMf2Values(Document doc) {
        assertXpathEvaluatesTo(
                "MERCIA MUDSTONE GROUP", "//gsml:MappedFeature[@gml:id='mf2']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:crs:EPSG::4326",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.3 52.6 -1.3 52.6 -1.2 52.5 -1.2 52.5 -1.3",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:CGI:EarthNaturalSurface",
                "//gsml:MappedFeature[@gml:id=\"mf2\"]/gsml:samplingFrame/@xlink:href",
                doc);
        // specification gu.25678
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification/gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo(
                "gu.25678",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        // description
        assertXpathEvaluatesTo(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:description",
                doc);
        // name
        assertXpathCount(
                3,
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name",
                doc);
        // multi-valued leaf attributes that are feature chained come in random order
        // when joining is used
        List<String> names = new ArrayList<String>();
        names.add("Yaugher Volcanic Group 1");
        names.add("Yaugher Volcanic Group 2");
        names.add("-Py");
        String name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification/gsml:GeologicUnit/gml:name[1]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification/gsml:GeologicUnit/gml:name[2]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification/gsml:GeologicUnit/gml:name[3]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

        assertXpathEvaluatesTo(
                "instance",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:purpose",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::unknown",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:geologicUnitType/@xlink:href",
                doc);
        // occurrence
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf2",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf3",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence[2]/@xlink:href",
                doc);
    }

    private void checkMf3Values(Document doc) {
        assertXpathEvaluatesTo(
                "CLIFTON FORMATION", "//gsml:MappedFeature[@gml:id='mf3']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:crs:EPSG::4326",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:CGI:EarthNaturalSurface",
                "//gsml:MappedFeature[@gml:id=\"mf3\"]/gsml:samplingFrame/@xlink:href",
                doc);
        // specification gu.25678
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo(
                "gu.25678",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        // description
        assertXpathEvaluatesTo(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:description",
                doc);
        // name
        assertXpathCount(
                3,
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name",
                doc);
        // multi-valued leaf attributes that are feature chained come in random order
        // when joining is used
        List<String> names = new ArrayList<String>();
        names.add("Yaugher Volcanic Group 1");
        names.add("Yaugher Volcanic Group 2");
        names.add("-Py");
        String name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/gsml:GeologicUnit/gml:name[1]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/gsml:GeologicUnit/gml:name[2]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/gsml:GeologicUnit/gml:name[3]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

        assertXpathEvaluatesTo(
                "instance",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:purpose",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::unknown",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:geologicUnitType/@xlink:href",
                doc);
        // occurrence
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf2",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf3",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence[2]/@xlink:href",
                doc);
    }

    private void checkMf4Values(Document doc, String epsgId)
            throws NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException,
                    TransformException {
        assertXpathEvaluatesTo(
                "MURRADUC BASALT", "//gsml:MappedFeature[@gml:id='mf4']/gml:name", doc);
        String srsName = "urn:ogc:def:crs:EPSG::" + (epsgId == null ? "4326" : epsgId);
        assertXpathEvaluatesTo(
                srsName,
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:shape/gml:Polygon/@srsName",
                doc);
        if (epsgId == null) {
            assertXpathEvaluatesTo(
                    "52.5 -1.3 52.6 -1.3 52.6 -1.2 52.5 -1.2 52.5 -1.3",
                    "//gsml:MappedFeature[@gml:id='mf4']/gsml:shape//gml:posList",
                    doc);
        } else {
            // I can't get exact transformation figures to compare with.. not important to test
            // anyway
        }

        assertXpathEvaluatesTo(
                "urn:cgi:feature:CGI:EarthNaturalSurface",
                "//gsml:MappedFeature[@gml:id=\"mf4\"]/gsml:samplingFrame/@xlink:href",
                doc);
        // specification gu.25682
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification/gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo(
                "gu.25682",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        // description
        assertXpathEvaluatesTo(
                "Olivine basalt",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:description",
                doc);
        // name
        assertXpathCount(
                2,
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name",
                doc);
        assertXpathEvaluatesTo(
                "New Group",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification"
                        + "/gsml:GeologicUnit/gml:name[@codeSpace='urn:ietf:rfc:2141']",
                doc);
        // multi-valued leaf attributes that are feature chained come in random order
        // when joining is used
        List<String> names = new ArrayList<String>();
        names.add("New Group");
        names.add("-Xy");
        String name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification/gsml:GeologicUnit/gml:name[1]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification/gsml:GeologicUnit/gml:name[2]",
                        doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

        assertXpathEvaluatesTo(
                "instance",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:purpose",
                doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::unknown",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:geologicUnitType/@xlink:href",
                doc);
        // occurrence
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf4",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification"
                        + "/gsml:GeologicUnit/gsml:occurrence/@xlink:href",
                doc);
    }

    @Test
    public void testWfs110GetFeature()
            throws MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException,
                    TransformException {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&outputFormat=gml32&maxFeatures=2&startIndex=2");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        // expecting mf3 and mf4
        assertXpathCount(2, "//gsml:MappedFeature", doc);

        // ensure order is correct too
        Node firstNode = doc.getElementsByTagName("gsml:MappedFeature").item(0);
        assertEquals("mf3", firstNode.getAttributes().getNamedItem("gml:id").getNodeValue());
        checkMf3Values(doc);

        Node secondNode = doc.getElementsByTagName("gsml:MappedFeature").item(1);
        assertEquals("mf4", secondNode.getAttributes().getNamedItem("gml:id").getNodeValue());
        checkMf4Values(doc, null);
    }

    @Test
    public void testWfs200GetFeature() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=2.0.0&typeNames=gsml:MappedFeature&count=1&startIndex=1");
        LOGGER.info("WFS GetFeature&typeNames=gsml:MappedFeature response:\n" + prettyString(doc));
        // expecting mf2
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf2", "//gsml:MappedFeature/@gml:id", doc);
        checkMf2Values(doc);
    }

    @Test
    public void testGetFeatureDenormalised() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=2.0.0&typeNames=gsml:GeologicUnit&count=1&startIndex=1");
        LOGGER.info("WFS GetFeature&typeNames=gsml:GeologicUnit response:\n" + prettyString(doc));
        // expecting gu.25682
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.25682", "//gsml:GeologicUnit/@gml:id", doc);
        // description
        assertXpathEvaluatesTo("Olivine basalt", "//gsml:GeologicUnit/gml:description", doc);
        // name
        assertXpathCount(2, "//gsml:GeologicUnit/gml:name", doc);
        assertXpathEvaluatesTo(
                "New Group", "//gsml:GeologicUnit/gml:name[@codeSpace='urn:ietf:rfc:2141']", doc);
        // multi-valued leaf attributes that are feature chained come in random order
        // when joining is used
        List<String> names = new ArrayList<String>();
        names.add("New Group");
        names.add("-Xy");
        String name = evaluate("//gsml:GeologicUnit/gml:name[1]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name = evaluate("//gsml:GeologicUnit/gml:name[2]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

        assertXpathEvaluatesTo("instance", "//gsml:GeologicUnit/gsml:purpose", doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::unknown",
                "//gsml:GeologicUnit/gsml:geologicUnitType/@xlink:href",
                doc);
        // occurrence
        assertXpathCount(1, "//gsml:GeologicUnit/gsml:occurrence", doc);
        assertXpathEvaluatesTo("", "//gsml:GeologicUnit/gsml:occurrence", doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf4",
                "//gsml:GeologicUnit/gsml:occurrence/@xlink:href",
                doc);
    }

    @Test
    public void testGetFeatureSortBy()
            throws MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException,
                    TransformException {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&outputFormat=gml32&maxFeatures=2&startIndex=2&sortBy=gsml:specification");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertXpathCount(2, "//gsml:MappedFeature", doc);

        // ensure order is correct too
        Node firstNode = doc.getElementsByTagName("gsml:MappedFeature").item(0);
        assertEquals("mf4", firstNode.getAttributes().getNamedItem("gml:id").getNodeValue());
        checkMf4Values(doc, null);

        Node secondNode = doc.getElementsByTagName("gsml:MappedFeature").item(1);
        assertEquals("mf1", secondNode.getAttributes().getNamedItem("gml:id").getNodeValue());
        checkMf1Values(doc);
    }

    @Test
    public void testGetFeatureSortByDenormalised() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:GeologicUnit&outputFormat=gml32&maxFeatures=2&startIndex=0&sortBy=gml:name");
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        assertXpathCount(2, "//gsml:GeologicUnit", doc);

        // ensure order is correct too
        Node firstNode = doc.getElementsByTagName("gsml:GeologicUnit").item(0);
        assertEquals("gu.25682", firstNode.getAttributes().getNamedItem("gml:id").getNodeValue());
        // description
        assertXpathEvaluatesTo(
                "Olivine basalt", "//gsml:GeologicUnit[@gml:id='gu.25682']/gml:description", doc);
        // name
        assertXpathCount(2, "//gsml:GeologicUnit[@gml:id='gu.25682']/gml:name", doc);
        assertXpathEvaluatesTo(
                "New Group",
                "//gsml:GeologicUnit[@gml:id='gu.25682']/gml:name[@codeSpace='urn:ietf:rfc:2141']",
                doc);
        // multi-valued leaf attributes that are feature chained come in random order
        // when joining is used
        List<String> names = new ArrayList<String>();
        names.add("New Group");
        names.add("-Xy");
        String name = evaluate("//gsml:GeologicUnit[@gml:id='gu.25682']/gml:name[1]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name = evaluate("//gsml:GeologicUnit[@gml:id='gu.25682']/gml:name[2]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

        assertXpathEvaluatesTo(
                "instance", "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:purpose", doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::unknown",
                "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:geologicUnitType/@xlink:href",
                doc);
        // occurrence
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:occurrence", doc);
        assertXpathEvaluatesTo("", "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:occurrence", doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf4",
                "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:occurrence/@xlink:href",
                doc);

        Node secondNode = doc.getElementsByTagName("gsml:GeologicUnit").item(1);
        assertEquals("gu.25699", secondNode.getAttributes().getNamedItem("gml:id").getNodeValue());
        // description
        assertXpathEvaluatesTo(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gml:description",
                doc);
        // name
        assertXpathCount(2, "//gsml:GeologicUnit[@gml:id='gu.25699']/gml:name", doc);
        assertXpathEvaluatesTo(
                "Yaugher Volcanic Group",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gml:name[@codeSpace='urn:ietf:rfc:2141']",
                doc);
        // multi-valued leaf attributes that are feature chained come in random order
        // when joining is used
        names.add("Yaugher Volcanic Group");
        names.add("-Py");
        name = evaluate("//gsml:GeologicUnit[@gml:id='gu.25699']/gml:name[1]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name = evaluate("//gsml:GeologicUnit[@gml:id='gu.25699']/gml:name[2]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

        assertXpathEvaluatesTo(
                "instance", "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:purpose", doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::unknown",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:geologicUnitType/@xlink:href",
                doc);
        // occurrence
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:occurrence", doc);
        assertXpathEvaluatesTo("", "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:occurrence", doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf1",
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:occurrence/@xlink:href",
                doc);
    }

    @Test
    public void testGetFeatureReproject()
            throws MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException,
                    TransformException {
        Document doc = null;
        doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&outputFormat=gml32&srsName=urn:ogc:def:crs:EPSG::4283&bbox=52.5,-1.3,52.51,-1.29&startIndex=1");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf4", "//gsml:MappedFeature/@gml:id", doc);
        checkMf4Values(doc, "4283");
    }

    @Test
    public void testGetFeatureWithFilter()
            throws MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException,
                    TransformException {
        String xml = //
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd " //
                        + AbstractAppSchemaMockData.GSML_URI
                        + " "
                        + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                        + "\" "
                        + "outputFormat=\"gml32\" "
                        + "startIndex=\"1\" "
                        + ">"
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "        <ogc:Filter>"
                        + "            <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
                        + "                <ogc:Literal>M*</ogc:Literal>"
                        + "                <ogc:PropertyName>gml:name</ogc:PropertyName>"
                        + "            </ogc:PropertyIsLike>"
                        + "        </ogc:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf4", "//gsml:MappedFeature/@gml:id", doc);
        checkMf4Values(doc, null);

        xml = //
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"2.0.0\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs/2.0 http://schemas.opengis.net/wfs/2.0/wfs.xsd " //
                        + AbstractAppSchemaMockData.GSML_URI
                        + " "
                        + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                        + "\" "
                        + "startIndex=\"0\" count=\"1\" "
                        + ">"
                        + "    <wfs:Query typeNames=\"gsml:GeologicUnit\">"
                        + "        <fes:Filter>"
                        + "            <fes:PropertyIsEqualTo>"
                        + "                <fes:Literal>-Py</fes:Literal>"
                        + "                <fes:ValueReference>gml:name</fes:ValueReference>"
                        + "            </fes:PropertyIsEqualTo>"
                        + "        </fes:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);

        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.25678", "//gsml:GeologicUnit/@gml:id", doc);
        // description
        assertXpathEvaluatesTo(
                "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                "//gsml:GeologicUnit/gml:description",
                doc);
        // name
        assertXpathCount(3, "//gsml:GeologicUnit/gml:name", doc);
        // multi-valued leaf attributes that are feature chained come in random order
        // when joining is used
        List<String> names = new ArrayList<String>();
        names.add("Yaugher Volcanic Group 1");
        names.add("Yaugher Volcanic Group 2");
        names.add("-Py");
        String name = evaluate("//gsml:GeologicUnit/gml:name[1]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name = evaluate("//gsml:GeologicUnit/gml:name[2]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        name = evaluate("//gsml:GeologicUnit/gml:name[3]", doc);
        assertTrue(names.contains(name));
        names.remove(name);
        assertTrue(names.isEmpty());

        assertXpathEvaluatesTo("instance", "//gsml:GeologicUnit/gsml:purpose", doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:nil:OGC::unknown",
                "//gsml:GeologicUnit/gsml:geologicUnitType/@xlink:href",
                doc);
        // occurrence
        assertXpathCount(2, "//gsml:GeologicUnit/gsml:occurrence", doc);
        assertXpathEvaluatesTo("", "//gsml:GeologicUnit/gsml:occurrence", doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf2",
                "//gsml:GeologicUnit/gsml:occurrence[1]/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:MappedFeature:mf3",
                "//gsml:GeologicUnit/gsml:occurrence[2]/@xlink:href",
                doc);
    }

    @Test
    public void testGetFeatureWithNestedFilter() {
        String xml = //
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd " //
                        + AbstractAppSchemaMockData.GSML_URI
                        + " "
                        + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                        + "\" "
                        + "outputFormat=\"gml32\" "
                        + "startIndex=\"1\" "
                        + ">"
                        + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                        + "        <ogc:Filter>"
                        + "            <ogc:PropertyIsEqualTo>"
                        + "                <ogc:Literal>Yaugher Volcanic Group 2</ogc:Literal>"
                        + "                <ogc:PropertyName>gsml:specification/gsml:GeologicUnit/gml:name</ogc:PropertyName>"
                        + "            </ogc:PropertyIsEqualTo>"
                        + "        </ogc:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf3", "//gsml:MappedFeature/@gml:id", doc);
        checkMf3Values(doc);

        // test WFS 2.0
        xml = //
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"2.0.0\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs/2.0 http://schemas.opengis.net/wfs/2.0/wfs.xsd " //
                        + AbstractAppSchemaMockData.GSML_URI
                        + " "
                        + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                        + "\" "
                        + "startIndex=\"0\" count=\"1\" "
                        + ">"
                        + "    <wfs:Query typeNames=\"gsml:MappedFeature\">"
                        + "        <fes:Filter>"
                        + "            <fes:PropertyIsEqualTo>"
                        + "                <fes:Literal>Yaugher Volcanic Group 2</fes:Literal>"
                        + "                <fes:ValueReference>gsml:specification/gsml:GeologicUnit/gml:name</fes:ValueReference>"
                        + "            </fes:PropertyIsEqualTo>"
                        + "        </fes:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf2", "//gsml:MappedFeature/@gml:id", doc);
        checkMf2Values(doc);

        // test xlink:href and post filtering (using functions)
        xml = //
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"2.0.0\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs/2.0 http://schemas.opengis.net/wfs/2.0/wfs.xsd " //
                        + AbstractAppSchemaMockData.GSML_URI
                        + " "
                        + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                        + "\" "
                        + ">"
                        + "    <wfs:Query typeNames=\"gsml:MappedFeature\">"
                        + "        <fes:Filter>"
                        + "            <fes:PropertyIsEqualTo>"
                        + "                <fes:Literal>urn:cgi:feature:MappedFeature:mf3</fes:Literal>"
                        + "                <fes:ValueReference>gsml:specification/gsml:GeologicUnit/gsml:occurrence/@xlink:href</fes:ValueReference>"
                        + "            </fes:PropertyIsEqualTo>"
                        + "        </fes:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(2, "//gsml:MappedFeature", doc);
        Node firstNode = doc.getElementsByTagName("gsml:MappedFeature").item(0);
        assertEquals("mf2", firstNode.getAttributes().getNamedItem("gml:id").getNodeValue());
        checkMf2Values(doc);

        Node secondNode = doc.getElementsByTagName("gsml:MappedFeature").item(1);
        assertEquals("mf3", secondNode.getAttributes().getNamedItem("gml:id").getNodeValue());
        assertXpathEvaluatesTo(
                "CLIFTON FORMATION", "//gsml:MappedFeature[@gml:id='mf3']/gml:name", doc);
        assertXpathEvaluatesTo(
                "urn:ogc:def:crs:EPSG::4326",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:shape//gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "urn:cgi:feature:CGI:EarthNaturalSurface",
                "//gsml:MappedFeature[@gml:id=\"mf3\"]/gsml:samplingFrame/@xlink:href",
                doc);
        // specification gu.25678
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification", doc);
        assertXpathCount(
                0, "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo(
                "#gu.25678",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href",
                doc);

        // test xlink:href and post filtering (using functions)
        xml = //
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"2.0.0\" " //
                        + "xmlns:fes=\"http://www.opengis.net/fes/2.0\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML-Core:3.0.0\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs/2.0 http://schemas.opengis.net/wfs/2.0/wfs.xsd " //
                        + AbstractAppSchemaMockData.GSML_URI
                        + " "
                        + AbstractAppSchemaMockData.GSML_SCHEMA_LOCATION_URL //
                        + "\" "
                        + "startIndex=\"1\" count=\"1\" "
                        + ">"
                        + "    <wfs:Query typeNames=\"gsml:MappedFeature\">"
                        + "        <fes:Filter>"
                        + "            <fes:PropertyIsEqualTo>"
                        + "                <fes:Literal>urn:cgi:feature:MappedFeature:mf3</fes:Literal>"
                        + "                <fes:ValueReference>gsml:specification/gsml:GeologicUnit/gsml:occurrence/@xlink:href</fes:ValueReference>"
                        + "            </fes:PropertyIsEqualTo>"
                        + "        </fes:Filter>"
                        + "    </wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf3", "//gsml:MappedFeature/@gml:id", doc);
        checkMf3Values(doc);
    }

    @Test
    public void testGetFeatureWithCSVFormat() throws Exception {
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=GetFeature&version=2.0.0&typeNames=gsml:MappedFeature&count=1&startIndex=1&outputFormat=csv");

        // check the mime type
        assertEquals("text/csv", resp.getContentType());

        // check the content disposition
        assertEquals(
                "attachment; filename=MappedFeature.csv", resp.getHeader("Content-Disposition"));

        // read the response back with a parser that can handle escaping, newlines and what not
        List<String[]> lines = CSVOutputFormatTest.readLines(resp.getContentAsString());

        // we should have one header line and then all the features in that feature type
        assertEquals(2, lines.size());

        assertEquals("mf2", lines.get(1)[0]);
    }

    @Test
    public void testGetMap() throws IOException {
        InputStream is =
                getBinary(
                        "wms?request=GetMap&SRS=EPSG:4326&layers=gsml:MappedFeature&styles=namefilter&BBOX=-2,52,0,54&X=0&Y=0&width=20&height=20&FORMAT=image/png&startIndex=1");
        BufferedImage imageBuffer = ImageIO.read(is);
        assertNotBlank("app-schema test getmap", imageBuffer, Color.WHITE);
        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("/test-data/img/mappedfeature.png")),
                imageBuffer,
                10);
    }
}
