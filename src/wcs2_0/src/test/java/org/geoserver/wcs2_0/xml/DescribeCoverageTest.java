/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.xml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.PreventLocalEntityResolver;
import org.junit.Test;
import org.w3c.dom.Document;

public class DescribeCoverageTest extends WCSTestSupport {

    public static QName WORLD = new QName(MockData.CITE_URI, "world", MockData.CITE_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        Map<SystemTestData.LayerProperty, Object> props = new HashMap<>();
        props.put(
                SystemTestData.LayerProperty.ENVELOPE,
                new ReferencedEnvelope(180, -180, 90, -90, null));
        testData.setUpRasterLayer(WORLD, "/world.tiff", null, null, WCSTestSupport.class);
    }

    @Test
    public void testNativeBoundsMissingSRS() throws Exception {
        final File xml =
                new File("./src/test/resources/testDescribeCoverageNativeBoundsMissingSRS.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        Document dom = postAsDOM("wcs", request);
        assertNotNull(dom);
        // print(dom, System.out);

        // validate
        checkValidationErrors(dom, getWcs20Schema());

        // check it is good
        assertXpathEvaluatesTo("cite__world", "//wcs:CoverageDescription//wcs:CoverageId", dom);
        assertXpathEvaluatesTo(
                "2", "//wcs:CoverageDescription//gml:boundedBy//gml:Envelope/@srsDimension", dom);
    }

    @Test
    public void testEntityExpansion() throws Exception {
        final File xml = new File("./src/test/resources/testDescribeCoverageEntityExpansion.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        Document dom = postAsDOM("wcs", request);
        assertNotNull(dom);
        // print(dom, System.out);

        String text = xpath.evaluate("//ows:ExceptionText", dom);
        assertTrue(text.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
    }

    @Test
    public void testDescribeCoverageSimple() throws Exception {

        final File xml = new File("./src/test/resources/testDescribeCoverage.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        Document dom = postAsDOM("wcs", request);
        assertNotNull(dom);
        // print(dom, System.out);

        // validate
        checkValidationErrors(dom, getWcs20Schema());

        // check it is good
        assertXpathEvaluatesTo("wcs__BlueMarble", "//wcs:CoverageDescription//wcs:CoverageId", dom);
        assertXpathEvaluatesTo(
                "3",
                "count(//wcs:CoverageDescription//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
        // enforce pixel center
        assertXpathEvaluatesTo(
                "-43.0020833333312 146.5020833333281",
                "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//gml:domainSet//gml:RectifiedGrid//gml:origin//gml:Point//gml:pos",
                dom);
    }

    @Test
    public void testDescribeCoverageMultiband() throws Exception {
        final File xml = new File("./src/test/resources/testDescribeCoverageMultiBand.xml");
        final String request = FileUtils.readFileToString(xml, "UTF-8");

        Document dom = postAsDOM("wcs", request);
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());

        // check it is good
        assertXpathEvaluatesTo("wcs__multiband", "//wcs:CoverageDescription//wcs:CoverageId", dom);
        assertXpathEvaluatesTo(
                "9",
                "count(//wcs:CoverageDescription//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }
}
