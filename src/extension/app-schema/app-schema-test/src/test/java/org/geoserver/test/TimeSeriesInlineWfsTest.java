/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * This is to test using isList to group multiple values as a concatenated single value without
 * feature chaining.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class TimeSeriesInlineWfsTest extends TimeSeriesWfsTest {

    protected TimeSeriesInlineMockData createTestData() {
        // only the test data is different since the config is slightly different (not using feature
        // chaining)
        // but the test cases from TimeSeriesWfsTest are the same
        return new TimeSeriesInlineMockData();
    }

    /** Test subsetting timePositionList. */
    @Test
    public void testTimePositionSubset() {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "outputFormat=\"gml32\" " //
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:csml=\""
                        + TimeSeriesMockData.CSML_URI
                        + "\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd" //
                        + "\"" //
                        + ">" //
                        + "<wfs:Query typeName=\"csml:PointSeriesFeature\">"
                        + "    <ogc:Filter>"
                        + "        <ogc:PropertyIsBetween>"
                        + "             <ogc:PropertyName>csml:PointSeriesFeature/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList</ogc:PropertyName>"
                        + "             <ogc:LowerBoundary><ogc:Literal>1949-05-01</ogc:Literal></ogc:LowerBoundary>"
                        + "             <ogc:UpperBoundary><ogc:Literal>1949-09-01</ogc:Literal></ogc:UpperBoundary>"
                        + "        </ogc:PropertyIsBetween>"
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathCount(1, "//csml:PointSeriesFeature", doc);
        checkPointFeatureTwo(doc);

        // HACK HACK HACK
        // The result is a subset of the timePositionList value that matches the filter
        // This is an experimental/temporary solution for Bureau of Meteorology subsetting
        // requirement
        assertXpathEvaluatesTo(
                "1949-05-01 1949-06-01 1949-07-01 1949-08-01 1949-09-01",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList",
                doc);

        // matching subset for QuantityList without feature chaining
        assertXpathEvaluatesTo(
                "16.2 17.1 22.0 25.1 23.9",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList",
                doc);
        // END OF HACK
    }

    @Override
    /** Test filtering quantity list that is not feature chained. */
    @Test
    public void testQuantityListSubset() {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "outputFormat=\"gml32\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:csml=\""
                        + TimeSeriesMockData.CSML_URI
                        + "\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd" //
                        + "\"" //
                        + ">" //
                        + "<wfs:Query typeName=\"csml:PointSeriesFeature\">"
                        + "    <ogc:Filter>"
                        + "        <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"\\\">"
                        + "            <ogc:PropertyName>csml:PointSeriesFeature/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList</ogc:PropertyName>"
                        + "            <ogc:Literal>*16.2*</ogc:Literal>"
                        + "        </ogc:PropertyIsLike>"
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathCount(1, "//csml:PointSeriesFeature", doc);
        checkPointFeatureTwo(doc);
        // subsetting works without feature chaining, therefore a subset of QuantityList expected
        assertXpathEvaluatesTo(
                "16.2",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList",
                doc);

        // matching subset of timePositionList expected
        assertXpathEvaluatesTo(
                "1949-05-01",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList",
                doc);
    }
}
