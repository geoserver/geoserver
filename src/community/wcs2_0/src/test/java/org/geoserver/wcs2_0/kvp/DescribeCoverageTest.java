package org.geoserver.wcs2_0.kvp;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertNotNull;

import javax.xml.namespace.QName;

import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DescribeCoverageTest extends WCSTestSupport {
    protected final static String DESCRIBE_URL = "wcs?service=WCS&version="+VERSION+"&request=DescribeCoverage";
    
    private static final QName MOSAIC = new QName(MockData.SF_URI, "rasterFilter", MockData.SF_PREFIX);

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(MOSAIC, "raster-filter-test.zip", null, getCatalog());
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
    }

    @Test
    public void testBasicKVP() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__BlueMarble");
        assertNotNull(dom);
        // print(dom, System.out);
        
        checkValidationErrors(dom, WCS20_SCHEMA);
        assertXpathEvaluatesTo("3", "count(//wcs:CoverageDescription//gmlcov:rangeType//swe:DataRecord//swe:field)", dom);
        assertXpathEvaluatesTo("image/tiff", "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat", dom);
    }
    
    @Test
    public void testMultiBandKVP() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__multiband");
        assertNotNull(dom);
        //        print(dom, System.out);
        
        checkValidationErrors(dom, WCS20_SCHEMA);       
        assertXpathEvaluatesTo("9", "count(//wcs:CoverageDescription//gmlcov:rangeType//swe:DataRecord//swe:field)", dom);        
        assertXpathEvaluatesTo("image/tiff", "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat", dom);
    }
    
    @Test
    public void testMultipleCoverages() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__multiband,wcs__BlueMarble");
        assertNotNull(dom);
        // print(dom, System.out);
        
        checkValidationErrors(dom, WCS20_SCHEMA);       
        assertXpathEvaluatesTo("2", "count(//wcs:CoverageDescription)", dom);        
        assertXpathEvaluatesTo("wcs__multiband", "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]/@gml:id", dom);
        assertXpathEvaluatesTo("9", "count(//wcs:CoverageDescription[1]//gmlcov:rangeType//swe:DataRecord//swe:field)", dom);
        assertXpathEvaluatesTo("image/tiff", "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat", dom);
        assertXpathEvaluatesTo("wcs__BlueMarble", "//wcs:CoverageDescriptions/wcs:CoverageDescription[2]/@gml:id", dom);
        assertXpathEvaluatesTo("3", "count(//wcs:CoverageDescription[2]//gmlcov:rangeType//swe:DataRecord//swe:field)", dom);
        assertXpathEvaluatesTo("image/tiff", "//wcs:CoverageDescriptions/wcs:CoverageDescription[2]//wcs:ServiceParameters//wcs:nativeFormat", dom);
    }
    
    @Test
    public void testMultipleCoveragesOneNotExists() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(DESCRIBE_URL + "&coverageId=wcs__multiband,wcs__IAmNotThere");

        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");
    }
    
    @Test
    public void testNativeFormatMosaic() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__rasterFilter");
        
        assertXpathEvaluatesTo("1", "count(//wcs:CoverageDescription)", dom);        
        assertXpathEvaluatesTo("sf__rasterFilter", "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]/@gml:id", dom);
        assertXpathEvaluatesTo("3", "count(//wcs:CoverageDescription[1]//gmlcov:rangeType//swe:DataRecord//swe:field)", dom);
        assertXpathEvaluatesTo("image/tiff", "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat", dom);
    }
    
    @Test
    public void testNativeFormatArcGrid() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__rain");
        // print(dom);
        
        assertXpathEvaluatesTo("1", "count(//wcs:CoverageDescription)", dom);        
        assertXpathEvaluatesTo("sf__rain", "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]/@gml:id", dom);
        assertXpathEvaluatesTo("1", "count(//wcs:CoverageDescription[1]//gmlcov:rangeType//swe:DataRecord//swe:field)", dom);
        assertXpathEvaluatesTo("text/plain", "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat", dom);
    }
        
}
