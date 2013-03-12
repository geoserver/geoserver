package org.geoserver.wcs2_0.kvp;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertNotNull;

import javax.xml.namespace.QName;

import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DescribeCoverageTest extends WCSTestSupport {
    protected final static String DESCRIBE_URL = "wcs?service=WCS&version="+VERSION+"&request=DescribeCoverage";
    
    private static final QName MOSAIC = new QName(MockData.SF_URI, "rasterFilter", MockData.SF_PREFIX);

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);
    
    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    
    protected static QName TIMERANGES = new QName(MockData.SF_URI, "timeranges", MockData.SF_PREFIX);
    
    @Before
    public void clearDimensions() {
        clearDimensions(getLayerId(WATTEMP));
        clearDimensions(getLayerId(TIMERANGES));
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(MOSAIC, "raster-filter-test.zip", null, getCatalog());
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
        testData.addRasterLayer(WATTEMP, "watertemp.zip", null, null, SystemTestData.class, getCatalog());
        testData.addRasterLayer(TIMERANGES, "timeranges.zip", null, null, SystemTestData.class, getCatalog());
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
    
    @Test
    public void testDescribeTimeList() throws Exception {
        setupRasterDimension(getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__watertemp");
        // print(dom);
        
        checkWaterTempEnvelope(dom);
        
        // check that metadata contains a list of times
        assertXpathEvaluatesTo("2", "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant)", dom);
        assertXpathEvaluatesTo("sf__watertemp_td_0", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[1]/@gml:id", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[1]/gml:timePosition", dom);
        assertXpathEvaluatesTo("sf__watertemp_td_1", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[2]/@gml:id", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00.000Z", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[2]/gml:timePosition", dom);
    }
    
    @Test
    public void testDescribeTimeContinousInterval() throws Exception {
        setupRasterDimension(getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.CONTINUOUS_INTERVAL, null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__watertemp");
        print(dom);
        
        checkWaterTempEnvelope(dom);
        
        // check that metadata contains a list of times
        assertXpathEvaluatesTo("1", "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod)", dom);
        assertXpathEvaluatesTo("sf__watertemp_tp_0", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/@gml:id", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:beginPosition", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00.000Z", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:endPosition", dom);
        assertXpathEvaluatesTo("0", "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:TimeInterval)", dom);
    }
    
    @Test
    public void testDescribeTimeDiscreteInterval() throws Exception {
        setupRasterDimension(getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.DISCRETE_INTERVAL, 1000 * 60 * 60 * 24d);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__watertemp");
        print(dom);
        
        checkWaterTempEnvelope(dom);
        
        // check that metadata contains a list of times
        assertXpathEvaluatesTo("1", "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod)", dom);
        assertXpathEvaluatesTo("sf__watertemp_tp_0", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/@gml:id", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:beginPosition", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00.000Z", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:endPosition", dom);
        assertXpathEvaluatesTo("1", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:TimeInterval", dom);
        assertXpathEvaluatesTo("day", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:TimeInterval/@unit", dom);
    }
    
    @Test
    public void testDescribeTimeRangeList() throws Exception {
        setupRasterDimension(getLayerId(TIMERANGES), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__timeranges");
        // print(dom);
        
        // check the envelope with time
        assertXpathEvaluatesTo("1", "count(//gml:boundedBy/gml:EnvelopeWithTimePeriod)", dom);
        assertXpathEvaluatesTo("lat lon time", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@axisLabels", dom);
        assertXpathEvaluatesTo("Deg Deg s", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@uomLabels", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:beginPosition", dom);
        assertXpathEvaluatesTo("2008-11-07T00:00:00.000Z", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:endPosition", dom);
        
        // check that metadata contains a list of times
        assertXpathEvaluatesTo("2", "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod)", dom);
        assertXpathEvaluatesTo("sf__timeranges_td_0", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[1]/@gml:id", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[1]/gml:beginPosition", dom);
        assertXpathEvaluatesTo("2008-11-04T00:00:00.000Z", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[1]/gml:endPosition", dom);
        assertXpathEvaluatesTo("sf__timeranges_td_1", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[2]/@gml:id", dom);
        assertXpathEvaluatesTo("2008-11-05T00:00:00.000Z", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[2]/gml:beginPosition", dom);
        assertXpathEvaluatesTo("2008-11-07T00:00:00.000Z", "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[2]/gml:endPosition", dom);
    }

    private void checkWaterTempEnvelope(Document dom) throws XpathException {
        // check the envelope with time
        assertXpathEvaluatesTo("1", "count(//gml:boundedBy/gml:EnvelopeWithTimePeriod)", dom);
        assertXpathEvaluatesTo("lat lon time", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@axisLabels", dom);
        assertXpathEvaluatesTo("Deg Deg s", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@uomLabels", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:beginPosition", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00.000Z", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:endPosition", dom);
    }
        
}
