/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import static org.custommonkey.xmlunit.XMLAssert.*;
import static org.geoserver.data.test.MockData.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs.test.WCSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DescribeCoverageTest extends WCSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
    }

    @Before
    public void revertTasmaniaDem() throws IOException {
        getTestData().addDefaultRasterLayer(TASMANIA_DEM, getCatalog());
    }

    @Test
    public void testDescribeAll() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=DescribeCoverage&service=WCS&version=1.0.0");
        // print(dom);
        // the response is compliant
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        // check all coverages have been described
        int count = getCatalog().getCoverages().size();
        assertEquals(count, dom.getElementsByTagName("wcs:CoverageOffering").getLength());
    }

    @Test
    public void testSkipMisconfigured() throws Exception {
        // enable skipping of misconfigured layers
        GeoServerInfo global = getGeoServer().getGlobal();
        global.setResourceErrorHandling(ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS);
        getGeoServer().save(global);
        // manually misconfigure one layer
        CoverageStoreInfo cvInfo =
                getCatalog().getCoverageStoreByName(MockData.TASMANIA_DEM.getLocalPart());
        cvInfo.setURL("file:///I/AM/NOT/THERE");
        getCatalog().save(cvInfo);

        Document dom = getAsDOM(BASEPATH + "?request=DescribeCoverage&service=WCS&version=1.0.0");
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        int count = getCatalog().getCoverages().size();
        assertEquals(count - 1, dom.getElementsByTagName("wcs:CoverageOffering").getLength());
    }

    @Test
    public void testDescribeUnknownCoverageKvp() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage=plop");
        // print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ServiceException").item(0);
        assertEquals("InvalidParameterValue", element.getAttribute("code"));
        assertEquals("coverage", element.getAttribute("locator"));
        assertTrue(element.getTextContent().contains("plop"));
    }

    @Test
    public void testDescribeMissingVersion() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&coverage="
                                + getLayerId(TASMANIA_DEM));
        // print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ServiceException").item(0);
        assertEquals("MissingParameterValue", element.getAttribute("code"));
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
                        "  xmlns:wcs=\"http://www.opengis.net/wcs\"\r\n"
                        + //
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
                        + //
                        "  version=\"1.0.0\" >\r\n"
                        + //
                        "  <wcs:Coverage>plop</wcs:Coverage>\r\n"
                        + //
                        "</wcs:DescribeCoverage>";
        Document dom = postAsDOM(BASEPATH, request, errors);
        // print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ServiceException").item(0);
        assertEquals("InvalidParameterValue", element.getAttribute("code"));
        assertEquals("coverage", element.getAttribute("locator"));
        assertTrue(element.getTextContent().contains("plop"));
    }

    @Test
    public void testMetadataLinks() throws Exception {

        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        MetadataLinkInfo ml = catalog.getFactory().createMetadataLink();
        ml.setContent("http://www.geoserver.org/tasmania/dem.xml");
        ml.setMetadataType("FGDC");
        ml.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(ml);
        catalog.save(ci);

        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                                + getLayerId(TASMANIA_DEM));
        print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        assertXpathEvaluatesTo("http://www.geoserver.org", "//wcs:metadataLink/@about", dom);
        assertXpathEvaluatesTo("FGDC", "//wcs:metadataLink/@metadataType", dom);
        assertXpathEvaluatesTo("simple", "//wcs:metadataLink/@xlink:type", dom);
        assertXpathEvaluatesTo(
                "http://www.geoserver.org/tasmania/dem.xml", "//wcs:metadataLink/@xlink:href", dom);
    }

    @Test
    public void testMetadataLinksTransormToProxyBaseURL() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        MetadataLinkInfo ml = catalog.getFactory().createMetadataLink();
        ml.setContent("/metadata?key=value");
        ml.setMetadataType("FGDC");
        ml.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(ml);
        catalog.save(ci);

        String proxyBaseUrl = getGeoServer().getGlobal().getSettings().getProxyBaseUrl();
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                                + getLayerId(TASMANIA_DEM));
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        assertXpathEvaluatesTo("http://www.geoserver.org", "//wcs:metadataLink/@about", dom);
        assertXpathEvaluatesTo("FGDC", "//wcs:metadataLink/@metadataType", dom);
        assertXpathEvaluatesTo("simple", "//wcs:metadataLink/@xlink:type", dom);
        assertXpathEvaluatesTo(
                proxyBaseUrl + "/metadata?key=value", "//wcs:metadataLink/@xlink:href", dom);
    }

    @Test
    public void testDescribeDemCoverageKvp() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                                + getLayerId(TASMANIA_DEM));
        // print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        checkDemCoverageDescription(dom);
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
                        "  xmlns:wcs=\"http://www.opengis.net/wcs\"\r\n"
                        + //
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
                        + //
                        "  version=\"1.0.0\" >\r\n"
                        + //
                        "  <wcs:Coverage>"
                        + getLayerId(TASMANIA_DEM)
                        + "</wcs:Coverage>\r\n"
                        + //
                        "</wcs:DescribeCoverage>";
        Document dom = postAsDOM(BASEPATH, request, errors);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        checkDemCoverageDescription(dom);
    }

    private void checkDemCoverageDescription(Document dom) throws Exception {
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageOffering").getLength());
        assertXpathEvaluatesTo(
                getLayerId(TASMANIA_DEM),
                "/wcs:CoverageDescription/wcs:CoverageOffering/wcs:name",
                dom);
        // check there is no rotation
        Node gridOffsets =
                xpath.getMatchingNodes(
                                "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                        + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                                dom)
                        .item(0);
        String[] offsetStrsLow = gridOffsets.getTextContent().split(" ");
        gridOffsets =
                xpath.getMatchingNodes(
                                "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                        + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                                dom)
                        .item(1);
        String[] offsetStrsHigh = gridOffsets.getTextContent().split(" ");
        assertEquals(2, offsetStrsLow.length);
        assertEquals(2, offsetStrsHigh.length);
        double[] offsets = new double[4];
        for (int i = 0; i < offsetStrsLow.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrsLow[i]);
        }
        for (int i = 2; i < 2 + offsetStrsHigh.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrsHigh[i - 2]);
        }
        assertTrue(offsets[0] > 0);
        assertEquals(0.0, offsets[1]);
        assertEquals(0.0, offsets[2]);
        assertTrue(offsets[3] < 0);
        // check there is one axis (this one is a dem, just one band)
        assertEquals(1, dom.getElementsByTagName("wcs:AxisDescription").getLength());
    }

    @Test
    public void testDescribeRotatedCoverage() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                                + getLayerId(ROTATED_CAD));
        // print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageOffering").getLength());
        assertXpathEvaluatesTo(
                getLayerId(ROTATED_CAD),
                "/wcs:CoverageDescription/wcs:CoverageOffering/wcs:name",
                dom);
        // check there is rotation
        Node gridOffsets =
                xpath.getMatchingNodes(
                                "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                        + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                                dom)
                        .item(0);
        String[] offsetStrsLow = gridOffsets.getTextContent().split(" ");
        gridOffsets =
                xpath.getMatchingNodes(
                                "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                        + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                                dom)
                        .item(1);
        String[] offsetStrsHigh = gridOffsets.getTextContent().split(" ");
        assertEquals(2, offsetStrsLow.length);
        assertEquals(2, offsetStrsHigh.length);
        double[] offsets = new double[4];
        for (int i = 0; i < offsetStrsLow.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrsLow[i]);
        }
        for (int i = 2; i < 2 + offsetStrsHigh.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrsHigh[i - 2]);
        }
        // System.out.println(Arrays.toString(offsets));
        assertTrue(offsets[0] < 0);
        assertTrue(offsets[1] > 0);
        assertTrue(offsets[2] > 0);
        assertTrue(offsets[3] > 0);
        // check there is one axis (this one is a dem, just one band)
        assertEquals(1, dom.getElementsByTagName("wcs:AxisDescription").getLength());
    }

    @Test
    public void testDescribeImageCoverage() throws Exception {
        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                                + getLayerId(TASMANIA_BM));
        // print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageOffering").getLength());
        assertXpathEvaluatesTo(
                getLayerId(TASMANIA_BM),
                "/wcs:CoverageDescription/wcs:CoverageOffering/wcs:name",
                dom);
        // check there is no rotation
        Node gridOffsets =
                xpath.getMatchingNodes(
                                "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                        + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                                dom)
                        .item(0);
        String[] offsetStrsLow = gridOffsets.getTextContent().split(" ");
        gridOffsets =
                xpath.getMatchingNodes(
                                "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                        + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                                dom)
                        .item(1);
        String[] offsetStrsHigh = gridOffsets.getTextContent().split(" ");
        assertEquals(2, offsetStrsLow.length);
        assertEquals(2, offsetStrsHigh.length);
        double[] offsets = new double[4];
        for (int i = 0; i < offsetStrsLow.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrsLow[i]);
        }
        for (int i = 2; i < 2 + offsetStrsHigh.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrsHigh[i - 2]);
        }
        assertTrue(offsets[0] > 0);
        assertEquals(0.0, offsets[1]);
        assertEquals(0.0, offsets[2]);
        assertTrue(offsets[3] < 0);

        // check there is one axis (this one is a dem, just one band)
        assertEquals(1, dom.getElementsByTagName("wcs:AxisDescription").getLength());

        // make sure we got the 3 bands
        assertEquals(1, dom.getElementsByTagName("wcs:interval").getLength());
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                        + //
                        "<wcs:DescribeCoverage service=\"WCS\" "
                        + //
                        "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n"
                        + //
                        "  xmlns:wcs=\"http://www.opengis.net/wcs\"\r\n"
                        + //
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
                        + //
                        "  version=\"1.0.0\" >\r\n"
                        + //
                        "  <wcs:Coverage>"
                        + TASMANIA_DEM.getLocalPart()
                        + "</wcs:Coverage>\r\n"
                        + //
                        "</wcs:DescribeCoverage>";
        Document dom = postAsDOM("cdf/wcs", request);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());

        dom = postAsDOM("wcs", request);
        assertEquals("wcs:CoverageDescription", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testLayerQualified() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                        + //
                        "<wcs:DescribeCoverage service=\"WCS\" "
                        + //
                        "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n"
                        + //
                        "  xmlns:wcs=\"http://www.opengis.net/wcs\"\r\n"
                        + //
                        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n"
                        + //
                        "  version=\"1.0.0\" >\r\n"
                        + //
                        "  <wcs:Coverage>"
                        + TASMANIA_DEM.getLocalPart()
                        + "</wcs:Coverage>\r\n"
                        + //
                        "</wcs:DescribeCoverage>";
        Document dom = postAsDOM("wcs/World/wcs", request);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());

        dom = postAsDOM("wcs/DEM/wcs", request);
        assertEquals("wcs:CoverageDescription", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testTimeCoverageList() throws Exception {
        setupRasterDimension(WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null);

        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                                + getLayerId(WATTEMP));
        // print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);

        // check the envelopes
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z", "//wcs:lonLatEnvelope/gml:timePosition[1]", dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z", "//wcs:lonLatEnvelope/gml:timePosition[2]", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gml:EnvelopeWithTimePeriod/gml:timePosition[1]",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gml:EnvelopeWithTimePeriod/gml:timePosition[2]",
                dom);

        // check the temporal domain
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z", "//wcs:temporalDomain/gml:timePosition[1]", dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z", "//wcs:temporalDomain/gml:timePosition[2]", dom);
    }

    @Test
    public void testTimeCoverageContinousInterval() throws Exception {
        setupRasterDimension(
                WATTEMP, ResourceInfo.TIME, DimensionPresentation.CONTINUOUS_INTERVAL, null);

        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                                + getLayerId(WATTEMP));
        print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);

        // check the envelopes
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z", "//wcs:lonLatEnvelope/gml:timePosition[1]", dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z", "//wcs:lonLatEnvelope/gml:timePosition[2]", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gml:EnvelopeWithTimePeriod/gml:timePosition[1]",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gml:EnvelopeWithTimePeriod/gml:timePosition[2]",
                dom);

        // check the temporal domain
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//wcs:temporalDomain/wcs:timePeriod/wcs:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//wcs:temporalDomain/wcs:timePeriod/wcs:endPosition",
                dom);
    }

    @Test
    public void testTimeCoverageDiscreteInterval() throws Exception {
        setupRasterDimension(
                WATTEMP,
                ResourceInfo.TIME,
                DimensionPresentation.DISCRETE_INTERVAL,
                Double.valueOf(1000 * 60 * 60));

        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                                + getLayerId(WATTEMP));
        // print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);

        // check the envelopes
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z", "//wcs:lonLatEnvelope/gml:timePosition[1]", dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z", "//wcs:lonLatEnvelope/gml:timePosition[2]", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gml:EnvelopeWithTimePeriod/gml:timePosition[1]",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gml:EnvelopeWithTimePeriod/gml:timePosition[2]",
                dom);

        // check the temporal domain
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//wcs:temporalDomain/wcs:timePeriod/wcs:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//wcs:temporalDomain/wcs:timePeriod/wcs:endPosition",
                dom);
        assertXpathEvaluatesTo(
                "PT1H", "//wcs:temporalDomain/wcs:timePeriod/wcs:timeResolution", dom);
    }

    @Test
    public void testElevationList() throws Exception {
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);

        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                                + getLayerId(WATTEMP));
        print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);

        // check the elevation list (it's the only way we can present it)
        assertXpathEvaluatesTo(
                "0.0",
                "//wcs:AxisDescription[wcs:name = 'ELEVATION']/wcs:values/wcs:singleValue[1]",
                dom);
        assertXpathEvaluatesTo(
                "100.0",
                "//wcs:AxisDescription[wcs:name = 'ELEVATION']/wcs:values/wcs:singleValue[2]",
                dom);
        assertXpathEvaluatesTo(
                "0.0", "//wcs:AxisDescription[wcs:name = 'ELEVATION']/wcs:values/wcs:default", dom);
    }

    @Test
    public void testTimeRangeCoverageList() throws Exception {
        setupRasterDimension(TIMERANGES, ResourceInfo.TIME, DimensionPresentation.LIST, null);

        Document dom =
                getAsDOM(
                        BASEPATH
                                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                                + getLayerId(TIMERANGES));
        print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);

        // check the envelopes
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z", "//wcs:lonLatEnvelope/gml:timePosition[1]", dom);
        assertXpathEvaluatesTo(
                "2008-11-07T00:00:00.000Z", "//wcs:lonLatEnvelope/gml:timePosition[2]", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gml:EnvelopeWithTimePeriod/gml:timePosition[1]",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-07T00:00:00.000Z",
                "//gml:EnvelopeWithTimePeriod/gml:timePosition[2]",
                dom);

        // check the temporal domain
        assertXpathEvaluatesTo("2", "count(//wcs:temporalDomain/wcs:timePeriod)", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//wcs:temporalDomain/wcs:timePeriod[1]/wcs:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-04T00:00:00.000Z",
                "//wcs:temporalDomain/wcs:timePeriod[1]/wcs:endPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-05T00:00:00.000Z",
                "//wcs:temporalDomain/wcs:timePeriod[2]/wcs:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-07T00:00:00.000Z",
                "//wcs:temporalDomain/wcs:timePeriod[2]/wcs:endPosition",
                dom);
    }

    @Test
    public void testMethodNameInjection() throws Exception {
        Document dom =
                getAsDOM(
                        "wcs?service=WCS&version=1.0.0"
                                + "&request=DescribeCoverage%22%3E%3C/ServiceException%3E%3Cfoo%3EHello,%20World%3C/foo%3E%3CServiceException+foo=%22"
                                + "&coverage="
                                + getLayerId(TIMERANGES));
        // print(dom);

        // check we have a valid exception
        XMLAssert.assertXpathExists("/ServiceExceptionReport/ServiceException", dom);
        XMLAssert.assertXpathEvaluatesTo(
                "OperationNotSupported", "/ServiceExceptionReport/ServiceException/@code", dom);
        // the locator has been escaped
        XMLAssert.assertXpathEvaluatesTo(
                "DescribeCoverage\"></ServiceException><foo>Hello, World</foo><ServiceException foo=\"",
                "/ServiceExceptionReport/ServiceException/@locator",
                dom);
        // the attack failed and the foo element is not there
        XMLAssert.assertXpathNotExists("//foo", dom);
    }
}
