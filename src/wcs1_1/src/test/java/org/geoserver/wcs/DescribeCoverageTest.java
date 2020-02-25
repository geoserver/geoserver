/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.ROTATED_CAD;
import static org.geoserver.data.test.MockData.TASMANIA_BM;
import static org.geoserver.data.test.MockData.TASMANIA_DEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs.test.WCSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DescribeCoverageTest extends WCSTestSupport {

    public static QName NO_RANGE = new QName(MockData.WCS_URI, "NoRange", MockData.WCS_PREFIX);
    private static final QName SF_RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);
    private static final QName GS_RAIN =
            new QName(MockData.DEFAULT_URI, "rain", MockData.DEFAULT_PREFIX);

    // @Override
    // protected String getDefaultLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    // public void testCRS() throws NoSuchAuthorityCodeException, FactoryException {
    // System.out.println(CRS.decode("EPSG:4326"));
    // System.out.println(CRS.decode("urn:ogc:def:crs:EPSG:4326"));
    // }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addRasterLayer(SF_RAIN, "rain.zip", "asc", getCatalog());
        testData.addRasterLayer(GS_RAIN, "rain.zip", "asc", getCatalog());
        testData.addRasterLayer(
                NO_RANGE, "norange.tiff", null, null, DescribeCoverageTest.class, getCatalog());
        // the GUI builds the dimension without range, let's do the same here
        CoverageInfo noRange = getCatalog().getCoverageByName(getLayerId(NO_RANGE));
        CoverageDimensionInfo cdi = noRange.getDimensions().get(0);
        cdi.setRange(null);
        getCatalog().save(noRange);

        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
    }

    @Before
    public void revertTasmaniaDem() throws IOException {
        getTestData().addDefaultRasterLayer(TASMANIA_DEM, getCatalog());
    }

    @Test
    public void testDescribeNoIdentifiers() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=DescribeCoverage&service=WCS&version=1.1.1");
        // print(dom);
        assertEquals(1, dom.getElementsByTagName("ows:ExceptionReport").getLength());
        Element element = (Element) dom.getElementsByTagName("ows:Exception").item(0);
        assertEquals("MissingParameterValue", element.getAttribute("exceptionCode"));
        assertEquals("identifiers", element.getAttribute("locator"));
    }

    @Test
    public void testDescribeByIdentifiers() throws Exception {
        String queryString =
                "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                        + "&BoundingBox=-45,146,-42,149,urn:ogc:def:crs:EPSG:6.6:4326";
        // Get identifiers from getCapabilities
        Document dom = getAsDOM("wcs?request=GetCapabilities&service=WCS");
        NodeList nodes =
                xpath.getMatchingNodes(
                        "//wcs:CoverageSummary/wcs:Identifier[text()[contains(.,'rain')]]", dom);
        assertTrue(nodes.getLength() >= 2);

        for (int i = 0; i < nodes.getLength(); i++) {
            String identifier = nodes.item(i).getTextContent();
            dom =
                    getAsDOM(
                            "wcs?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                                    + identifier);
            // Response should be a valid document consisting of 1 coverage with a matching
            // identifier
            print(dom);
            assertEquals("wcs:CoverageDescriptions", dom.getDocumentElement().getNodeName());
            assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
            assertEquals(
                    identifier,
                    dom.getElementsByTagName("wcs:Identifier").item(0).getTextContent());
        }
    }

    @Test
    public void testDescribeUnknownCoverageKvp() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers=plop");
        // print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ows:Exception").item(0);
        assertEquals("InvalidParameterValue", element.getAttribute("exceptionCode"));
        assertEquals("identifiers", element.getAttribute("locator"));
        assertTrue(element.getTextContent().contains("plop"));
    }

    @Test
    public void testDescribeMissingVersion() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&identifiers="
                                + getLayerId(TASMANIA_DEM));
        // print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ows:Exception").item(0);
        assertEquals("MissingParameterValue", element.getAttribute("exceptionCode"));
        assertEquals("version", element.getAttribute("locator"));
    }

    @Test
    public void testDescribeUnknownCoverageXml() throws Exception {
        List<Exception> errors = new ArrayList<Exception>();
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                        + //
                        "<wcs:DescribeCoverage service=\"WCS\" "
                        + //
                        "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n"
                        + //
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\"\r\n"
                        + //
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
                        + //
                        "  version=\"1.1.1\" >\r\n"
                        + //
                        "  <wcs:Identifier>plop</wcs:Identifier>\r\n"
                        + //
                        "</wcs:DescribeCoverage>";
        Document dom = postAsDOM(BASEPATH, request, errors);
        // print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ows:Exception").item(0);
        assertEquals("InvalidParameterValue", element.getAttribute("exceptionCode"));
        assertEquals("identifiers", element.getAttribute("locator"));
        assertTrue(element.getTextContent().contains("plop"));
    }

    @Test
    public void testDescribeDemCoverageKvp() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                                + getLayerId(TASMANIA_DEM));
        checkValidationErrors(dom, WCS11_SCHEMA);
        checkDemCoverageDescription(dom);
    }

    @Test
    public void testDescribeNoRangeKvp() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                                + getLayerId(NO_RANGE));
        print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);

        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescriptions").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertXpathEvaluatesTo(
                getLayerId(NO_RANGE),
                "/wcs:CoverageDescriptions/wcs:CoverageDescription/wcs:Identifier",
                dom);
        // check we generated a ows:AnyValue for the field definition (since we have no validity
        // range)
        assertXpathEvaluatesTo("1", "count(//wcs:Field/wcs:Definition/ows:AnyValue)", dom);
    }

    @Test
    public void testDescribeDemCoverageXml() throws Exception {
        List<Exception> errors = new ArrayList<Exception>();
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                        + //
                        "<wcs:DescribeCoverage service=\"WCS\" "
                        + //
                        "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n"
                        + //
                        "  xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\"\r\n"
                        + //
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
                        + //
                        "  version=\"1.1.1\" >\r\n"
                        + //
                        "  <wcs:Identifier>"
                        + getLayerId(TASMANIA_DEM)
                        + "</wcs:Identifier>\r\n"
                        + //
                        "</wcs:DescribeCoverage>";
        Document dom = postAsDOM(BASEPATH, request, errors);
        checkValidationErrors(dom, WCS11_SCHEMA);
        checkDemCoverageDescription(dom);
    }

    private void checkDemCoverageDescription(Document dom) throws Exception {
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescriptions").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertXpathEvaluatesTo(
                getLayerId(TASMANIA_DEM),
                "/wcs:CoverageDescriptions/wcs:CoverageDescription/wcs:Identifier",
                dom);
        // check there is no rotation
        Node gridOffsets =
                xpath.getMatchingNodes(
                                "/wcs:CoverageDescriptions/wcs:CoverageDescription/"
                                        + "wcs:Domain/wcs:SpatialDomain/wcs:GridCRS/wcs:GridOffsets",
                                dom)
                        .item(0);
        String[] offsetStrs = gridOffsets.getTextContent().split(" ");
        assertEquals(4, offsetStrs.length);
        double[] offsets = new double[4];
        for (int i = 0; i < offsetStrs.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrs[i]);
        }
        assertTrue(offsets[0] > 0);
        assertEquals(0.0, offsets[1], 0d);
        assertEquals(0.0, offsets[2], 0d);
        assertTrue(offsets[3] < 0);
        // check there is one field, one axis, one key (this one is a dem, just one band)
        assertEquals(1, dom.getElementsByTagName("wcs:Field").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:Axis").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:Key").getLength());
    }

    @Test
    public void testDescribeRotatedCoverage() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                                + getLayerId(ROTATED_CAD));
        // print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescriptions").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertXpathEvaluatesTo(
                getLayerId(ROTATED_CAD),
                "/wcs:CoverageDescriptions/wcs:CoverageDescription/wcs:Identifier",
                dom);
        // check there is no rotation
        Node gridOffsets =
                xpath.getMatchingNodes(
                                "/wcs:CoverageDescriptions/wcs:CoverageDescription/"
                                        + "wcs:Domain/wcs:SpatialDomain/wcs:GridCRS/wcs:GridOffsets",
                                dom)
                        .item(0);
        String[] offsetStrs = gridOffsets.getTextContent().split(" ");
        assertEquals(4, offsetStrs.length);
        double[] offsets = new double[4];
        for (int i = 0; i < offsetStrs.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrs[i]);
        }
        // System.out.println(Arrays.toString(offsets));
        assertTrue(offsets[0] < 0);
        assertTrue(offsets[1] > 0);
        assertTrue(offsets[2] > 0);
        assertTrue(offsets[3] > 0);
        // check there is one field, one axis, one key (this one is a dem, just one band)
        assertEquals(1, dom.getElementsByTagName("wcs:Field").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:Axis").getLength());
        assertEquals(4, dom.getElementsByTagName("wcs:Key").getLength());
    }

    @Test
    public void testDescribeImageCoverage() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                                + getLayerId(TASMANIA_BM));
        // print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescriptions").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertXpathEvaluatesTo(
                getLayerId(TASMANIA_BM),
                "/wcs:CoverageDescriptions/wcs:CoverageDescription/wcs:Identifier",
                dom);
        // check there is no rotation
        Node gridOffsets =
                xpath.getMatchingNodes(
                                "/wcs:CoverageDescriptions/wcs:CoverageDescription/"
                                        + "wcs:Domain/wcs:SpatialDomain/wcs:GridCRS/wcs:GridOffsets",
                                dom)
                        .item(0);
        String[] offsetStrs = gridOffsets.getTextContent().split(" ");
        assertEquals(4, offsetStrs.length);
        double[] offsets = new double[4];
        for (int i = 0; i < offsetStrs.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrs[i]);
        }
        assertTrue(offsets[0] > 0);
        assertEquals(0.0, offsets[1], 0d);
        assertEquals(0.0, offsets[2], 0d);
        assertTrue(offsets[3] < 0);

        // check there is one field, one axis, three keys (bands)
        assertEquals(1, dom.getElementsByTagName("wcs:Field").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:Axis").getLength());
        assertEquals(3, dom.getElementsByTagName("wcs:Key").getLength());

        // make sure key names do not have spaces inside
        NodeList keys = dom.getElementsByTagName("wcs:Key");
        for (int i = 0; i < keys.getLength(); i++) {
            Node key = keys.item(i);
            assertFalse(key.getTextContent().contains(" "));
        }

        // make sure the field name is "contents" (just a reasonable default)
        assertXpathEvaluatesTo(
                "contents",
                "/wcs:CoverageDescriptions/wcs:CoverageDescription/"
                        + "wcs:Range/wcs:Field/wcs:Identifier",
                dom);
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        Document dom =
                getAsDOM(
                        "cdf/wcs?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                                + TASMANIA_DEM.getLocalPart());
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());

        dom =
                getAsDOM(
                        "wcs?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                                + TASMANIA_DEM.getLocalPart());
        assertEquals("wcs:CoverageDescriptions", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testMetadataLink() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        MetadataLinkInfo ml = catalog.getFactory().createMetadataLink();
        ml.setContent("http://www.geoserver.org/tasmania/dem.xml");
        ml.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(ml);
        catalog.save(ci);

        Document dom =
                getAsDOM(
                        "wcs?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                                + TASMANIA_DEM.getLocalPart());
        // print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);
        String xpathBase = "//wcs:CoverageDescription/ows:Metadata";
        assertXpathEvaluatesTo("http://www.geoserver.org", xpathBase + "/@about", dom);
        assertXpathEvaluatesTo("simple", xpathBase + "/@xlink:type", dom);
        assertXpathEvaluatesTo(
                "http://www.geoserver.org/tasmania/dem.xml", xpathBase + "/@xlink:href", dom);
    }

    @Test
    public void testMetadataLinksTransormToProxyBaseURL() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        MetadataLinkInfo ml = catalog.getFactory().createMetadataLink();
        ml.setContent("/metadata?key=value");
        ml.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(ml);
        catalog.save(ci);

        String proxyBaseUrl = getGeoServer().getGlobal().getSettings().getProxyBaseUrl();
        Document dom =
                getAsDOM(
                        "wcs?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                                + TASMANIA_DEM.getLocalPart());
        checkValidationErrors(dom, WCS11_SCHEMA);
        String xpathBase = "//wcs:CoverageDescription/ows:Metadata";
        assertXpathEvaluatesTo("http://www.geoserver.org", xpathBase + "/@about", dom);
        assertXpathEvaluatesTo("simple", xpathBase + "/@xlink:type", dom);
        assertXpathEvaluatesTo(
                proxyBaseUrl + "/metadata?key=value", xpathBase + "/@xlink:href", dom);
    }
}
