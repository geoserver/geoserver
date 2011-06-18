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
import org.w3c.dom.NodeList;

public class DescribeCoverageTest extends WCSTestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new DescribeCoverageTest());
    }

//    @Override
//    protected String getDefaultLogConfiguration() {
//        return "/DEFAULT_LOGGING.properties";
//    }
    
//    public void testCRS() throws NoSuchAuthorityCodeException, FactoryException {
//        System.out.println(CRS.decode("EPSG:4326"));
//        System.out.println(CRS.decode("urn:ogc:def:crs:EPSG:4326"));
//    }
    
    public void testDescribeNoIdentifiers() throws Exception {
        Document dom = getAsDOM(BASEPATH + "?request=DescribeCoverage&service=WCS&version=1.1.1");
//        print(dom);
        assertEquals(1, dom.getElementsByTagName("ows:ExceptionReport").getLength());
        Element element = (Element) dom.getElementsByTagName("ows:Exception").item(0);
        assertEquals("MissingParameterValue", element.getAttribute("exceptionCode"));
        assertEquals("identifiers", element.getAttribute("locator"));
    }

    public void testDescribeUnknownCoverageKvp() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers=plop");
//        print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ows:Exception").item(0);
        assertEquals("InvalidParameterValue", element.getAttribute("exceptionCode"));
        assertEquals("identifiers", element.getAttribute("locator"));
        assertTrue(element.getTextContent().contains("plop"));
    }
    
    public void testDescribeMissingVersion() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=DescribeCoverage&service=WCS&identifiers=" 
                + getLayerId(TASMANIA_DEM));
//        print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ows:Exception").item(0);
        assertEquals("MissingParameterValue", element.getAttribute("exceptionCode"));
        assertEquals("version", element.getAttribute("locator"));
    }
    
    public void testDescribeUnknownCoverageXml() throws Exception {
        List<Exception> errors = new ArrayList<Exception>();
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + // 
            "<wcs:DescribeCoverage service=\"WCS\" " + //
            "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n" + // 
            "  xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\"\r\n" + // 
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + // 
            "  version=\"1.1.1\" >\r\n" + //
            "  <wcs:Identifier>plop</wcs:Identifier>\r\n" + // 
            "</wcs:DescribeCoverage>";
        Document dom = postAsDOM(BASEPATH, request, errors);
//        print(dom);
        checkOws11Exception(dom);
        Element element = (Element) dom.getElementsByTagName("ows:Exception").item(0);
        assertEquals("InvalidParameterValue", element.getAttribute("exceptionCode"));
        assertEquals("identifiers", element.getAttribute("locator"));
        assertTrue(element.getTextContent().contains("plop"));
    }

    public void testDescribeDemCoverageKvp() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                + getLayerId(TASMANIA_DEM));
//        print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);
        checkDemCoverageDescription(dom);
    }
    
    public void testDescribeDemCoverageXml() throws Exception {
        List<Exception> errors = new ArrayList<Exception>();
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + // 
            "<wcs:DescribeCoverage service=\"WCS\" " + //
            "xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n" + // 
            "  xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\"\r\n" + // 
            "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + // 
            "  version=\"1.1.1\" >\r\n" + //
            "  <wcs:Identifier>" + getLayerId(TASMANIA_DEM) + "</wcs:Identifier>\r\n" + // 
            "</wcs:DescribeCoverage>";
        Document dom = postAsDOM(BASEPATH, request, errors);
        checkValidationErrors(dom, WCS11_SCHEMA);
        checkDemCoverageDescription(dom);
    }

    private void checkDemCoverageDescription(Document dom) throws Exception {
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescriptions").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertXpathEvaluatesTo(getLayerId(TASMANIA_DEM), 
                "/wcs:CoverageDescriptions/wcs:CoverageDescription/wcs:Identifier", dom);
        // check there is no rotation
        Node gridOffsets = xpath.getMatchingNodes("/wcs:CoverageDescriptions/wcs:CoverageDescription/" +
                "wcs:Domain/wcs:SpatialDomain/wcs:GridCRS/wcs:GridOffsets", dom).item(0);
        String[] offsetStrs = gridOffsets.getTextContent().split(" ");
        assertEquals(4, offsetStrs.length);
        double[] offsets = new double[4];
        for (int i = 0; i < offsetStrs.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrs[i]);
        }
        assertTrue(offsets[0] > 0);
        assertEquals(0.0, offsets[1]);
        assertEquals(0.0, offsets[2]);
        assertTrue(offsets[3] < 0);
        // check there is one field, one axis, one key (this one is a dem, just one band)
        assertEquals(1, dom.getElementsByTagName("wcs:Field").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:Axis").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:Key").getLength());
    }
    
    public void testDescribeRotatedCoverage() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                + getLayerId(ROTATED_CAD));
//        print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescriptions").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertXpathEvaluatesTo(getLayerId(ROTATED_CAD), 
                "/wcs:CoverageDescriptions/wcs:CoverageDescription/wcs:Identifier", dom);
        // check there is no rotation
        Node gridOffsets = xpath.getMatchingNodes("/wcs:CoverageDescriptions/wcs:CoverageDescription/" +
                "wcs:Domain/wcs:SpatialDomain/wcs:GridCRS/wcs:GridOffsets", dom).item(0);
        String[] offsetStrs = gridOffsets.getTextContent().split(" ");
        assertEquals(4, offsetStrs.length);
        double[] offsets = new double[4];
        for (int i = 0; i < offsetStrs.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrs[i]);
        }
//        System.out.println(Arrays.toString(offsets));
        assertTrue(offsets[0] < 0);
        assertTrue(offsets[1] > 0);
        assertTrue(offsets[2] > 0);
        assertTrue(offsets[3] > 0);
        // check there is one field, one axis, one key (this one is a dem, just one band)
        assertEquals(1, dom.getElementsByTagName("wcs:Field").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:Axis").getLength());
        assertEquals(4, dom.getElementsByTagName("wcs:Key").getLength());

    }
    
    public void testDescribeImageCoverage() throws Exception {
        Document dom = getAsDOM(BASEPATH
                + "?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="
                + getLayerId(TASMANIA_BM));
//        print(dom);
        checkValidationErrors(dom, WCS11_SCHEMA);
        // check the basics, the output is a single coverage description with the expected id
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescriptions").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:CoverageDescription").getLength());
        assertXpathEvaluatesTo(getLayerId(TASMANIA_BM), 
                "/wcs:CoverageDescriptions/wcs:CoverageDescription/wcs:Identifier", dom);
        // check there is no rotation
        Node gridOffsets = xpath.getMatchingNodes("/wcs:CoverageDescriptions/wcs:CoverageDescription/" +
                "wcs:Domain/wcs:SpatialDomain/wcs:GridCRS/wcs:GridOffsets", dom).item(0);
        String[] offsetStrs = gridOffsets.getTextContent().split(" ");
        assertEquals(4, offsetStrs.length);
        double[] offsets = new double[4];
        for (int i = 0; i < offsetStrs.length; i++) {
            offsets[i] = Double.parseDouble(offsetStrs[i]);
        }
        assertTrue(offsets[0] > 0);
        assertEquals(0.0, offsets[1]);
        assertEquals(0.0, offsets[2]);
        assertTrue(offsets[3] < 0);
        
        // check there is one field, one axis, three keys (bands)
        assertEquals(1, dom.getElementsByTagName("wcs:Field").getLength());
        assertEquals(1, dom.getElementsByTagName("wcs:Axis").getLength());
        assertEquals(3, dom.getElementsByTagName("wcs:Key").getLength());
        
        // make sure key names do not have spaces inside
        NodeList keys = dom.getElementsByTagName("wcs:Key");
        for(int i = 0; i < keys.getLength(); i++) {
            Node key = keys.item(i);
            assertFalse(key.getTextContent().contains(" "));
        }
        
        // make sure the field name is "contents" (just a reasonable default)
        assertXpathEvaluatesTo("contents", "/wcs:CoverageDescriptions/wcs:CoverageDescription/" +
                "wcs:Range/wcs:Field/wcs:Identifier", dom);
    }
    
    public void testWorkspaceQualified() throws Exception {
        Document dom = getAsDOM(
            "cdf/wcs?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="+TASMANIA_DEM.getLocalPart());
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        
        dom = getAsDOM(
            "wcs?request=DescribeCoverage&service=WCS&version=1.1.1&identifiers="+TASMANIA_DEM.getLocalPart());
        assertEquals("wcs:CoverageDescriptions", dom.getDocumentElement().getNodeName());
    }
}
