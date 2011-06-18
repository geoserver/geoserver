package org.geoserver.wcs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.ROTATED_CAD;
import static org.geoserver.data.test.MockData.TASMANIA_BM;
import static org.geoserver.data.test.MockData.TASMANIA_DEM;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.geoserver.wcs.test.WCSTestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DescribeCoverageTest extends WCSTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new DescribeCoverageTest());
    }

    // @Override
    // protected String getDefaultLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    // public void testCRS() throws NoSuchAuthorityCodeException, FactoryException {
    // System.out.println(CRS.decode("EPSG:4326"));
    // System.out.println(CRS.decode("urn:ogc:def:crs:EPSG:4326"));
    // }

    public void testDescribeNoIdentifiers() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=DescribeCoverage&service=WCS&version=1.0.0");
        // print(dom);
        assertEquals(1, dom.getElementsByTagName("ServiceExceptionReport").getLength());
        Element element = (Element) dom.getElementsByTagName("ServiceException").item(0);
        assertEquals("MissingParameterValue", element.getAttribute("code"));
        assertEquals("coverage", element.getAttribute("locator"));
    }

    public void testDescribeUnknownCoverageKvp() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage=plop");
        // print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ServiceException").item(0);
        assertEquals("InvalidParameterValue", element.getAttribute("code"));
        assertEquals("coverage", element.getAttribute("locator"));
        assertTrue(element.getTextContent().contains("plop"));
    }

    public void testDescribeMissingVersion() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=DescribeCoverage&service=WCS&coverage="
                + getLayerId(TASMANIA_DEM));
        // print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ServiceException").item(0);
        assertEquals("MissingParameterValue", element.getAttribute("code"));
        assertEquals("version", element.getAttribute("locator"));
    }

    public void testDescribeUnknownCoverageXml() throws Exception {
        List<Exception> errors = new ArrayList<Exception>();
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + // 
                "<wcs:DescribeCoverage service=\"WCS\" " + //
                "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n" + // 
                "  xmlns:wcs=\"http://www.opengis.net/wcs\"\r\n" + // 
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + // 
                "  version=\"1.0.0\" >\r\n" + //
                "  <wcs:Coverage>plop</wcs:Coverage>\r\n" + // 
                "</wcs:DescribeCoverage>";
        Document dom = postAsDOM(BASEPATH, request, errors);
        // print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ServiceException").item(0);
        assertEquals("InvalidParameterValue", element.getAttribute("code"));
        assertEquals("coverage", element.getAttribute("locator"));
        assertTrue(element.getTextContent().contains("plop"));
    }

    public void testDescribeDemCoverageKvp() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                + getLayerId(TASMANIA_DEM));
        // print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        checkDemCoverageDescription(dom);
    }

    public void testDescribeDemCoverageXml() throws Exception {
        List<Exception> errors = new ArrayList<Exception>();
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + // 
                "<wcs:DescribeCoverage service=\"WCS\" " + //
                "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n" + // 
                "  xmlns:wcs=\"http://www.opengis.net/wcs\"\r\n" + // 
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + // 
                "  version=\"1.0.0\" >\r\n" + //
                "  <wcs:Coverage>" + getLayerId(TASMANIA_DEM) + "</wcs:Coverage>\r\n" + // 
                "</wcs:DescribeCoverage>";
        Document dom = postAsDOM(BASEPATH, request, errors);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        checkDemCoverageDescription(dom);
    }

    private void checkDemCoverageDescription(Document dom) throws Exception {
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageOffering").getLength());
        assertXpathEvaluatesTo(getLayerId(TASMANIA_DEM),
                "/wcs:CoverageDescription/wcs:CoverageOffering/wcs:name", dom);
        // check there is no rotation
        Node gridOffsets = xpath
                .getMatchingNodes(
                        "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                        dom).item(0);
        String[] offsetStrsLow = gridOffsets.getTextContent().split(" ");
        gridOffsets = xpath
                .getMatchingNodes(
                        "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                        dom).item(1);
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

    public void testDescribeRotatedCoverage() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                + getLayerId(ROTATED_CAD));
        // print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageOffering").getLength());
        assertXpathEvaluatesTo(getLayerId(ROTATED_CAD),
                "/wcs:CoverageDescription/wcs:CoverageOffering/wcs:name", dom);
        // check there is rotation
        Node gridOffsets = xpath
                .getMatchingNodes(
                        "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                        dom).item(0);
        String[] offsetStrsLow = gridOffsets.getTextContent().split(" ");
        gridOffsets = xpath
                .getMatchingNodes(
                        "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                        dom).item(1);
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

    public void testDescribeImageCoverage() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=DescribeCoverage&service=WCS&version=1.0.0&coverage="
                + getLayerId(TASMANIA_BM));
        // print(dom);
        checkValidationErrors(dom, WCS10_DESCRIBECOVERAGE_SCHEMA);
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageOffering").getLength());
        assertXpathEvaluatesTo(getLayerId(TASMANIA_BM),
                "/wcs:CoverageDescription/wcs:CoverageOffering/wcs:name", dom);
        // check there is no rotation
        Node gridOffsets = xpath
                .getMatchingNodes(
                        "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                        dom).item(0);
        String[] offsetStrsLow = gridOffsets.getTextContent().split(" ");
        gridOffsets = xpath
                .getMatchingNodes(
                        "/wcs:CoverageDescription/wcs:CoverageOffering/"
                                + "wcs:domainSet/wcs:spatialDomain/gml:RectifiedGrid/gml:offsetVector",
                        dom).item(1);
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
    
    public void testWorksapceQualified() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + // 
                "<wcs:DescribeCoverage service=\"WCS\" " + //
                "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n" + // 
                "  xmlns:wcs=\"http://www.opengis.net/wcs\"\r\n" + // 
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + // 
                "  version=\"1.0.0\" >\r\n" + //
                "  <wcs:Coverage>" + TASMANIA_DEM.getLocalPart() + "</wcs:Coverage>\r\n" + // 
                "</wcs:DescribeCoverage>";
        Document dom = postAsDOM("cdf/wcs", request);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        
        dom = postAsDOM("wcs", request);
        assertEquals("wcs:CoverageDescription", dom.getDocumentElement().getNodeName());
    }
    
    public void testLayerQualified() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + // 
                "<wcs:DescribeCoverage service=\"WCS\" " + //
                "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n" + // 
                "  xmlns:wcs=\"http://www.opengis.net/wcs\"\r\n" + // 
                "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + // 
                "  version=\"1.0.0\" >\r\n" + //
                "  <wcs:Coverage>" + TASMANIA_DEM.getLocalPart() + "</wcs:Coverage>\r\n" + // 
                "</wcs:DescribeCoverage>";
        Document dom = postAsDOM("wcs/World/wcs", request);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        
        dom = postAsDOM("wcs/DEM/wcs", request);
        assertEquals("wcs:CoverageDescription", dom.getDocumentElement().getNodeName());
    }
}
