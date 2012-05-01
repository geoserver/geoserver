/*
 * Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.w3c.dom.Document;

import junit.framework.Test;

/**
 * This is to test using isList to group multiple values as a concatenated single value without
 * feature chaining.
 * 
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */

public class TimeSeriesInlineWfsTest extends TimeSeriesWfsTest {
    /**
     * Read-only test so can use one-time setup.
     * 
     */
    public static Test suite() {
        return new OneTimeTestSetup(new TimeSeriesInlineWfsTest());
    }

    protected NamespaceTestData buildTestData() {
        // only the test data is different since the config is slightly different (not using feature
        // chaining)
        // but the test cases from TimeSeriesWfsTest are the same
        return new TimeSeriesInlineMockData();
    }

    @Override
    /**
     * Test filtering quantity list that is not feature chained.
     */
    public void testFilterQuantityList() {
        String xml = "<wfs:GetFeature "
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
                + "            <ogc:Literal>*16.2*</ogc:Literal>" + "        </ogc:PropertyIsLike>"
                + "    </ogc:Filter>" + "</wfs:Query> " + "</wfs:GetFeature>";
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathCount(1, "//csml:PointSeriesFeature", doc);
        checkPointFeatureTwo(doc, "ID2");
        assertXpathEvaluatesTo(
                "1949-05-01 1949-06-01 1949-07-01 1949-08-01 1949-09-01 1949-10-01 1949-11-01 1949-12-01 1950-01-01 1950-02-01"
                        + " 1950-03-01 1950-04-01 1950-05-01 1950-06-01 1950-07-01 1950-08-01 1950-09-01 1950-10-01 1950-11-01 1950-12-01",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList",
                doc);
        // subsetting works without feature chaining, therefore a subset of QuantityList expected
        assertXpathEvaluatesTo(
                "16.2",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList",
                doc);
    }
}
