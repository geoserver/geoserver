/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.TASMANIA_DEM;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class DescribeCoverageTest extends WCSTestSupport {
    protected static final String DESCRIBE_URL =
            "wcs?service=WCS&version=" + VERSION + "&request=DescribeCoverage";

    private static final QName MOSAIC =
            new QName(MockData.SF_URI, "rasterFilter", MockData.SF_PREFIX);

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);

    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    protected static QName TIMERANGES =
            new QName(MockData.SF_URI, "timeranges", MockData.SF_PREFIX);

    protected static QName MULTIDIM = new QName(MockData.SF_URI, "multidim", MockData.SF_PREFIX);

    protected static QName PK50095 = new QName(MockData.SF_URI, "pk50095", MockData.SF_PREFIX);

    @Before
    public void clearDimensions() {
        clearDimensions(getLayerId(WATTEMP));
        clearDimensions(getLayerId(TIMERANGES));
        clearDimensions(getLayerId(MULTIDIM));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(MOSAIC, "raster-filter-test.zip", null, getCatalog());
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
        testData.addRasterLayer(
                WATTEMP, "watertemp.zip", null, null, SystemTestData.class, getCatalog());
        testData.addRasterLayer(
                TIMERANGES, "timeranges.zip", null, null, SystemTestData.class, getCatalog());
        testData.addRasterLayer(
                MULTIDIM, "multidim.zip", null, null, SystemTestData.class, getCatalog());
        testData.addRasterLayer(
                PK50095, "wi-utm.zip", null, null, SystemTestData.class, getCatalog());
    }

    @Test
    public void testBasicKVP() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__BlueMarble");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo(
                "3",
                "count(//wcs:CoverageDescription//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void testProjectectedKVP() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__utm11");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo("E N", "//gml:boundedBy/gml:Envelope/@axisLabels", dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//wcs:CoverageDescription//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void testCustomUnit() throws Exception {
        CoverageInfo ciRain = getCatalog().getCoverageByName(getLayerId(RAIN));
        ciRain.getDimensions().get(0).setUnit("mm");
        getCatalog().save(ciRain);

        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__rain");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo(
                "1",
                "count(//wcs:CoverageDescription/gmlcov:rangeType/swe:DataRecord/swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "rain",
                "//wcs:CoverageDescription/gmlcov:rangeType//swe:DataRecord/swe:field/@name",
                dom);
        assertXpathEvaluatesTo(
                "mm",
                "//wcs:CoverageDescription/gmlcov:rangeType/swe:DataRecord/swe:field/swe:Quantity/swe:uom/@code",
                dom);
        assertXpathEvaluatesTo(
                "text/plain",
                "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void testAxisOrderUtm() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__pk50095");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo(
                "347660.5162105911 5191763.949937257",
                "//gml:boundedBy/gml:Envelope/gml:lowerCorner",
                dom);
        assertXpathEvaluatesTo(
                "353440.1129425911 5196950.767517257",
                "//gml:boundedBy/gml:Envelope/gml:upperCorner",
                dom);
        assertXpathEvaluatesTo(
                "+1 +2",
                "//gml:coverageFunction/gml:GridFunction/gml:sequenceRule/@axisOrder",
                dom);
        assertXpathEvaluatesTo(
                "347671.1015525911 5196940.182175256",
                "//gml:domainSet/gml:RectifiedGrid/gml:origin/gml:Point/gml:pos",
                dom);
    }

    @Test
    public void testCustomNullValue() throws Exception {
        CoverageInfo ciRain = getCatalog().getCoverageByName(getLayerId(RAIN));
        CoverageDimensionImpl dimension = (CoverageDimensionImpl) ciRain.getDimensions().get(0);
        List<Double> nullValues = new ArrayList<Double>();
        nullValues.add(-999.9);
        dimension.setNullValues(nullValues);
        getCatalog().save(ciRain);

        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__rain");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo(
                "1",
                "count(//wcs:CoverageDescription/gmlcov:rangeType/swe:DataRecord/swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "rain",
                "//wcs:CoverageDescription/gmlcov:rangeType//swe:DataRecord/swe:field/@name",
                dom);
        assertXpathEvaluatesTo(
                "-999.9",
                "//wcs:CoverageDescription/gmlcov:rangeType/swe:DataRecord/swe:field/swe:Quantity/swe:nilValues/swe:NilValues/swe:nilValue",
                dom);
    }

    @Test
    public void testMultiBandKVP() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__multiband");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo(
                "9",
                "count(//wcs:CoverageDescription//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void testMultiBandKVPNoWs() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=multiband");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo(
                "9",
                "count(//wcs:CoverageDescription//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void testMultiBandKVPLocalWs() throws Exception {
        Document dom = getAsDOM("wcs/" + DESCRIBE_URL + "&coverageId=multiband");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo(
                "9",
                "count(//wcs:CoverageDescription//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions//wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void testMultipleCoverages() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__multiband,wcs__BlueMarble");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo("2", "count(//wcs:CoverageDescription)", dom);
        assertXpathEvaluatesTo(
                "wcs__multiband",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "9",
                "count(//wcs:CoverageDescription[1]//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
        assertXpathEvaluatesTo(
                "wcs__BlueMarble",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[2]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "3",
                "count(//wcs:CoverageDescription[2]//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[2]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void testMultipleCoveragesNoWs() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=multiband,wcs__BlueMarble");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo("2", "count(//wcs:CoverageDescription)", dom);
        assertXpathEvaluatesTo(
                "wcs__multiband",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "9",
                "count(//wcs:CoverageDescription[1]//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
        assertXpathEvaluatesTo(
                "wcs__BlueMarble",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[2]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "3",
                "count(//wcs:CoverageDescription[2]//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[2]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void testMultipleCoveragesLocalWs() throws Exception {
        Document dom = getAsDOM("wcs/" + DESCRIBE_URL + "&coverageId=multiband,BlueMarble");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
        assertXpathEvaluatesTo("2", "count(//wcs:CoverageDescription)", dom);
        assertXpathEvaluatesTo(
                "wcs__multiband",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "9",
                "count(//wcs:CoverageDescription[1]//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
        assertXpathEvaluatesTo(
                "wcs__BlueMarble",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[2]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "3",
                "count(//wcs:CoverageDescription[2]//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[2]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void testMultipleCoveragesOneNotExists() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(DESCRIBE_URL + "&coverageId=wcs__multiband,wcs__IAmNotThere");

        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");
    }

    @Test
    public void testNativeFormatMosaic() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__rasterFilter");

        assertXpathEvaluatesTo("1", "count(//wcs:CoverageDescription)", dom);
        assertXpathEvaluatesTo(
                "sf__rasterFilter",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "3",
                "count(//wcs:CoverageDescription[1]//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "image/tiff",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void gridCellCenterEnforce() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=wcs__BlueMarble");
        assertNotNull(dom);
        // print(dom, System.out);

        checkValidationErrors(dom, getWcs20Schema());
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
    public void testNativeFormatArcGrid() throws Exception {
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__rain");
        // print(dom);

        assertXpathEvaluatesTo("1", "count(//wcs:CoverageDescription)", dom);
        assertXpathEvaluatesTo(
                "sf__rain", "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]/@gml:id", dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//wcs:CoverageDescription[1]//gmlcov:rangeType//swe:DataRecord//swe:field)",
                dom);
        assertXpathEvaluatesTo(
                "text/plain",
                "//wcs:CoverageDescriptions/wcs:CoverageDescription[1]//wcs:ServiceParameters//wcs:nativeFormat",
                dom);
    }

    @Test
    public void testMetadata() throws Exception {
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        ci.setTitle("My Title");
        ci.setDescription("My Abstract");
        ci.getKeywords().add(0, new Keyword("my_keyword"));
        MetadataLinkInfo mdl1 = catalog.getFactory().createMetadataLink();
        mdl1.setContent("http://www.geoserver.org/tasmania/dem.xml");
        mdl1.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(mdl1);
        MetadataLinkInfo mdl2 = catalog.getFactory().createMetadataLink();
        mdl2.setContent("/metadata?key=value");
        mdl2.setAbout("http://www.geoserver.org");
        ci.getMetadataLinks().add(mdl2);
        catalog.save(ci);
        String coverageId = getLayerId(TASMANIA_DEM).replace(":", "__");
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=" + coverageId);
        // print(dom);

        checkValidationErrors(dom, getWcs20Schema());
        String base = "//wcs:CoverageDescriptions/wcs:CoverageDescription/";
        assertXpathEvaluatesTo("My Title", base + "gml:name", dom);
        assertXpathEvaluatesTo("My Abstract", base + "gml:description", dom);
        base += "gmlcov:metadata/gmlcov:Extension/";
        assertXpathEvaluatesTo("4", "count(" + base + "ows:Keywords/ows:Keyword)", dom);
        assertXpathEvaluatesTo("my_keyword", base + "ows:Keywords/ows:Keyword[1]", dom);
        assertXpathEvaluatesTo("2", "count(" + base + "ows:Metadata)", dom);
        assertXpathEvaluatesTo("http://www.geoserver.org", base + "ows:Metadata[1]/@about", dom);
        assertXpathEvaluatesTo("simple", base + "ows:Metadata[1]/@xlink:type", dom);
        assertXpathEvaluatesTo(
                "http://www.geoserver.org/tasmania/dem.xml",
                base + "ows:Metadata[1]/@xlink:href",
                dom);
        assertXpathEvaluatesTo("http://www.geoserver.org", base + "ows:Metadata[2]/@about", dom);
        assertXpathEvaluatesTo("simple", base + "ows:Metadata[2]/@xlink:type", dom);
        assertXpathEvaluatesTo(
                "src/test/resources/geoserver/metadata?key=value",
                base + "ows:Metadata[2]/@xlink:href",
                dom);
    }

    @Test
    public void testDescribeTimeList() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__watertemp");
        // print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        checkWaterTempTimeEnvelope(dom);

        // check that metadata contains a list of times
        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant)",
                dom);
        assertXpathEvaluatesTo(
                "sf__watertemp_td_0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[1]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[1]/gml:timePosition",
                dom);
        assertXpathEvaluatesTo(
                "sf__watertemp_td_1",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[2]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[2]/gml:timePosition",
                dom);
    }

    @Test
    public void testDescribeTimeContinousInterval() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP),
                ResourceInfo.TIME,
                DimensionPresentation.CONTINUOUS_INTERVAL,
                null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__watertemp");
        // print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        checkWaterTempTimeEnvelope(dom);

        // check that metadata contains a list of times
        assertXpathEvaluatesTo(
                "1",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod)",
                dom);
        assertXpathEvaluatesTo(
                "sf__watertemp_tp_0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:endPosition",
                dom);
        assertXpathEvaluatesTo(
                "0",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:timeInterval)",
                dom);
    }

    @Test
    public void testDescribeTimeDiscreteInterval() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP),
                ResourceInfo.TIME,
                DimensionPresentation.DISCRETE_INTERVAL,
                1000 * 60 * 60 * 24d);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__watertemp");
        checkValidationErrors(dom, getWcs20Schema());
        //        print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        checkWaterTempTimeEnvelope(dom);

        // check that metadata contains a list of times
        assertXpathEvaluatesTo(
                "1",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod)",
                dom);
        assertXpathEvaluatesTo(
                "sf__watertemp_tp_0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:endPosition",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:timeInterval",
                dom);
        assertXpathEvaluatesTo(
                "day",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod/gml:timeInterval/@unit",
                dom);
    }

    @Test
    public void testDescribeTimeRangeList() throws Exception {
        setupRasterDimension(
                getLayerId(TIMERANGES), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__timeranges");
        // print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        // check the envelope with time
        assertXpathEvaluatesTo("1", "count(//gml:boundedBy/gml:EnvelopeWithTimePeriod)", dom);
        assertXpathEvaluatesTo(
                "Lat Long time", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@axisLabels", dom);
        assertXpathEvaluatesTo(
                "Deg Deg s", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@uomLabels", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-07T00:00:00.000Z",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:endPosition",
                dom);

        // check that metadata contains a list of times
        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod)",
                dom);
        assertXpathEvaluatesTo(
                "sf__timeranges_td_0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[1]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[1]/gml:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-04T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[1]/gml:endPosition",
                dom);
        assertXpathEvaluatesTo(
                "sf__timeranges_td_1",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[2]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-05T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[2]/gml:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-07T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[2]/gml:endPosition",
                dom);
    }

    @Test
    public void testDescribeElevationDiscreteInterval() throws Exception {
        setupRasterDimension(
                getLayerId(TIMERANGES),
                ResourceInfo.ELEVATION,
                DimensionPresentation.DISCRETE_INTERVAL,
                50d,
                "m");
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__timeranges");
        //        print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        checkElevationRangesEnvelope(dom);

        // check that metadata contains a list of elevations
        assertXpathEvaluatesTo(
                "1",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range)",
                dom);
        assertXpathEvaluatesTo(
                "20.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range/wcsgs:start",
                dom);
        assertXpathEvaluatesTo(
                "150.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range/wcsgs:end",
                dom);
        assertXpathEvaluatesTo(
                "50.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range/wcsgs:Interval",
                dom);
        assertXpathEvaluatesTo(
                "m",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range/wcsgs:Interval/@unit",
                dom);
    }

    @Test
    public void testDescribeElevationContinuousInterval() throws Exception {
        setupRasterDimension(
                getLayerId(TIMERANGES),
                ResourceInfo.ELEVATION,
                DimensionPresentation.CONTINUOUS_INTERVAL,
                null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__timeranges");
        //        print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        checkElevationRangesEnvelope(dom);

        // check that metadata contains elevation range
        assertXpathEvaluatesTo(
                "1",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range)",
                dom);
        assertXpathEvaluatesTo(
                "20.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range/wcsgs:start",
                dom);
        assertXpathEvaluatesTo(
                "150.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range/wcsgs:end",
                dom);
    }

    @Test
    public void testDescribeElevationValuesList() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__watertemp");
        //        print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        checkWaterTempElevationEnvelope(dom);

        // check that metadata contains a list of elevations
        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:SingleValue)",
                dom);
        assertXpathEvaluatesTo(
                "0.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:SingleValue[1]",
                dom);
        assertXpathEvaluatesTo(
                "100.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:SingleValue[2]",
                dom);
    }

    @Test
    public void testDescribeElevationRangeList() throws Exception {
        setupRasterDimension(
                getLayerId(TIMERANGES), ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__timeranges");
        //        print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        checkElevationRangesEnvelope(dom);

        // check that metadata contains a list of elevations
        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range)",
                dom);
        assertXpathEvaluatesTo(
                "20.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range[1]/wcsgs:start",
                dom);
        assertXpathEvaluatesTo(
                "99.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range[1]/wcsgs:end",
                dom);
        assertXpathEvaluatesTo(
                "100.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range[2]/wcsgs:start",
                dom);
        assertXpathEvaluatesTo(
                "150.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range[2]/wcsgs:end",
                dom);
    }

    @Test
    public void testDescribeTimeElevationList() throws Exception {
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(
                getLayerId(WATTEMP), ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__watertemp");
        //        print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        checkWaterTempTimeElevationEnvelope(dom);

        // check that metadata contains a list of times
        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant)",
                dom);
        assertXpathEvaluatesTo(
                "sf__watertemp_td_0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[1]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[1]/gml:timePosition",
                dom);
        assertXpathEvaluatesTo(
                "sf__watertemp_td_1",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[2]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimeInstant[2]/gml:timePosition",
                dom);

        // check that metadata contains a list of elevations
        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:SingleValue)",
                dom);
        assertXpathEvaluatesTo(
                "0.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:SingleValue[1]",
                dom);
        assertXpathEvaluatesTo(
                "100.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:SingleValue[2]",
                dom);
    }

    @Test
    public void testDescribeCustomDimensionsList() throws Exception {
        setupRasterDimension(
                getLayerId(MULTIDIM), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(
                getLayerId(MULTIDIM), ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        setupRasterDimension(
                getLayerId(MULTIDIM),
                ResourceInfo.CUSTOM_DIMENSION_PREFIX + "WAVELENGTH",
                DimensionPresentation.LIST,
                null);
        setupRasterDimension(
                getLayerId(MULTIDIM),
                ResourceInfo.CUSTOM_DIMENSION_PREFIX + "DATE",
                DimensionPresentation.LIST,
                null);
        Document dom = getAsDOM(DESCRIBE_URL + "&coverageId=sf__multidim");
        //        print(dom);
        checkValidationErrors(dom, getWcs20Schema());

        checkTimeElevationRangesEnvelope(dom);

        // check that metadata contains a list of times
        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod)",
                dom);
        assertXpathEvaluatesTo(
                "sf__multidim_td_0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[1]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[1]/gml:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-04T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[1]/gml:endPosition",
                dom);

        assertXpathEvaluatesTo(
                "sf__multidim_td_1",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[2]/@gml:id",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-05T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[2]/gml:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-07T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:TimeDomain/gml:TimePeriod[2]/gml:endPosition",
                dom);

        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range)",
                dom);
        assertXpathEvaluatesTo(
                "20.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range[1]/wcsgs:start",
                dom);
        assertXpathEvaluatesTo(
                "99.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range[1]/wcsgs:end",
                dom);
        assertXpathEvaluatesTo(
                "100.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range[2]/wcsgs:start",
                dom);
        assertXpathEvaluatesTo(
                "150.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:ElevationDomain/wcsgs:Range[2]/wcsgs:end",
                dom);

        // check the additional domains
        assertXpathEvaluatesTo(
                "2", "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain)", dom);

        // Check the date domain
        assertXpathEvaluatesTo(
                "DATE", "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[1]/@name", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[1]/@default",
                dom);
        assertXpathEvaluatesTo(
                "3",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[1]/gml:TimeInstant)",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[1]/gml:TimeInstant[1]/gml:timePosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[1]/gml:TimeInstant[2]/gml:timePosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-05T00:00:00.000Z",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[1]/gml:TimeInstant[3]/gml:timePosition",
                dom);

        // check the waveLength range domain
        assertXpathEvaluatesTo(
                "WAVELENGTH",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[2]/@name",
                dom);
        assertXpathEvaluatesTo(
                "12", "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[2]/@default", dom);
        assertXpathEvaluatesTo(
                "2",
                "count(//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[2]/wcsgs:Range)",
                dom);
        assertXpathEvaluatesTo(
                "12.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[2]/wcsgs:Range[1]/wcsgs:start",
                dom);
        assertXpathEvaluatesTo(
                "24.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[2]/wcsgs:Range[1]/wcsgs:end",
                dom);
        assertXpathEvaluatesTo(
                "25.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[2]/wcsgs:Range[2]/wcsgs:start",
                dom);
        assertXpathEvaluatesTo(
                "80.0",
                "//gmlcov:metadata/gmlcov:Extension/wcsgs:DimensionDomain[2]/wcsgs:Range[2]/wcsgs:end",
                dom);
    }

    private void checkWaterTempTimeEnvelope(Document dom) throws XpathException {
        // check the envelope with time
        assertXpathEvaluatesTo("1", "count(//gml:boundedBy/gml:EnvelopeWithTimePeriod)", dom);
        assertXpathEvaluatesTo(
                "Lat Long time", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@axisLabels", dom);
        assertXpathEvaluatesTo(
                "Deg Deg s", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@uomLabels", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:endPosition",
                dom);
    }

    private void checkWaterTempTimeElevationEnvelope(Document dom) throws XpathException {
        // check the envelope with time
        assertXpathEvaluatesTo("1", "count(//gml:boundedBy/gml:EnvelopeWithTimePeriod)", dom);
        assertXpathEvaluatesTo(
                "Lat Long elevation time",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@axisLabels",
                dom);
        assertXpathEvaluatesTo(
                "Deg Deg m s", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@uomLabels", dom);
        assertXpathEvaluatesTo(
                "3", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@srsDimension", dom);
        assertXpathEvaluatesTo(
                "40.562080748421806 0.23722068851276978 0.0",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:lowerCorner",
                dom);
        assertXpathEvaluatesTo(
                "44.55808294568743 14.592757149389236 100.0",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:upperCorner",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-01T00:00:00.000Z",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:endPosition",
                dom);
    }

    private void checkWaterTempElevationEnvelope(Document dom) throws XpathException {
        // check the envelope with time
        assertXpathEvaluatesTo("1", "count(//gml:boundedBy/gml:Envelope)", dom);
        assertXpathEvaluatesTo(
                "Lat Long elevation", "//gml:boundedBy/gml:Envelope/@axisLabels", dom);
        assertXpathEvaluatesTo("Deg Deg m", "//gml:boundedBy/gml:Envelope/@uomLabels", dom);
        assertXpathEvaluatesTo("3", "//gml:boundedBy/gml:Envelope/@srsDimension", dom);
        assertXpathEvaluatesTo(
                "40.562080748421806 0.23722068851276978 0.0",
                "//gml:boundedBy/gml:Envelope/gml:lowerCorner",
                dom);
        assertXpathEvaluatesTo(
                "44.55808294568743 14.592757149389236 100.0",
                "//gml:boundedBy/gml:Envelope/gml:upperCorner",
                dom);
    }

    private void checkElevationRangesEnvelope(Document dom) throws XpathException {
        // check the envelope with time
        assertXpathEvaluatesTo("1", "count(//gml:boundedBy/gml:Envelope)", dom);
        assertXpathEvaluatesTo(
                "Lat Long elevation", "//gml:boundedBy/gml:Envelope/@axisLabels", dom);
        assertXpathEvaluatesTo("Deg Deg m", "//gml:boundedBy/gml:Envelope/@uomLabels", dom);
        assertXpathEvaluatesTo("3", "//gml:boundedBy/gml:Envelope/@srsDimension", dom);
        assertXpathEvaluatesTo(
                "40.562080748421806 0.23722068851276978 20.0",
                "//gml:boundedBy/gml:Envelope/gml:lowerCorner",
                dom);
        assertXpathEvaluatesTo(
                "44.55808294568743 14.592757149389236 150.0",
                "//gml:boundedBy/gml:Envelope/gml:upperCorner",
                dom);
    }

    private void checkTimeElevationRangesEnvelope(Document dom) throws XpathException {
        // check the envelope with time
        assertXpathEvaluatesTo("1", "count(//gml:boundedBy/gml:EnvelopeWithTimePeriod)", dom);
        assertXpathEvaluatesTo(
                "Lat Long elevation time",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@axisLabels",
                dom);
        assertXpathEvaluatesTo(
                "Deg Deg m s", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@uomLabels", dom);
        assertXpathEvaluatesTo(
                "3", "//gml:boundedBy/gml:EnvelopeWithTimePeriod/@srsDimension", dom);
        assertXpathEvaluatesTo(
                "40.562080748421806 0.23722068851276978 20.0",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:lowerCorner",
                dom);
        assertXpathEvaluatesTo(
                "44.55808294568743 14.592757149389236 150.0",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:upperCorner",
                dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:beginPosition",
                dom);
        assertXpathEvaluatesTo(
                "2008-11-07T00:00:00.000Z",
                "//gml:boundedBy/gml:EnvelopeWithTimePeriod/gml:endPosition",
                dom);
    }
}
