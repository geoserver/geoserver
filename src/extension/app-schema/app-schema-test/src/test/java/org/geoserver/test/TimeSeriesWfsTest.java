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
 * This is to test using isList to group multiple values as a concatenated single value with feature
 * chaining.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class TimeSeriesWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new TimeSeriesMockData();
    }

    @Test
    public void testGetFeature() {
        String path = "wfs?request=GetFeature&outputFormat=gml32&typeName=csml:PointSeriesFeature";
        Document doc = getAsDOM(path);
        LOGGER.info(
                "WFS GetFeature, typename=csml:PointSeriesFeature response:\n" + prettyString(doc));
        validateGet(path);

        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathCount(2, "//csml:PointSeriesFeature", doc);

        String id = "ID1";
        assertXpathEvaluatesTo(id, "(//csml:PointSeriesFeature)[1]/@gml:id", doc);

        // location
        assertXpathEvaluatesTo(
                "42.58 31.29",
                "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:location",
                doc);
        assertXpathEvaluatesTo(
                "epsg:27700",
                "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:location/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "metre metre",
                "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:location/@uomLabels",
                doc);
        assertXpathEvaluatesTo(
                "Easting Northing",
                "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:location/@axisLabels",
                doc);
        // PointSeriesCoverage
        assertXpathCount(1, "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:value", doc);
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage",
                doc);
        assertXpathEvaluatesTo(
                "ID1.1",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/@gml:id",
                doc);
        // TimeSeries
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain",
                doc);
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries",
                doc);
        assertXpathEvaluatesTo(
                "ID1.2",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/@gml:id",
                doc);

        // timePositionList
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList",
                doc);

        assertXpathEvaluatesTo(
                "1948-01-01 1948-02-01 1948-03-01 1948-04-01 1948-05-01 1948-06-01 1948-07-01 1948-08-01 1948-09-01 1948-10-01"
                        + " 1948-11-01 1948-12-01 1949-01-01 1949-02-01 1949-03-01 1949-04-01",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList",
                doc);

        // quantityList
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet",
                doc);
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray",
                doc);
        assertXpathEvaluatesTo(
                "ID1.3",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/@gml:id",
                doc);
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent",
                doc);
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList",
                doc);

        assertXpathEvaluatesTo(
                "missing missing 8.9 7.9 14.2 15.4 18.1 19.1 21.7 20.8 19.6 14.9 10.8 8.8 8.5 10.4",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList",
                doc);

        assertXpathEvaluatesTo(
                "degC",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList/@uom",
                doc);

        // parameter xlink:href
        assertXpathEvaluatesTo(
                "http://cf-pcmdi.llnl.gov/documents/cf-standard-names/standard-name-table/current/cf-standard-name-table.html#surface_temperature",
                "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:parameter/@xlink:href",
                doc);
        id = "ID2";
        assertXpathEvaluatesTo(id, "(//csml:PointSeriesFeature)[2]/@gml:id", doc);
        checkPointFeatureTwo(doc);
        // check full timePositionList value
        assertXpathEvaluatesTo(
                "1949-05-01 1949-06-01 1949-07-01 1949-08-01 1949-09-01 1949-10-01 1949-11-01 1949-12-01 1950-01-01 1950-02-01"
                        + " 1950-03-01 1950-04-01 1950-05-01 1950-06-01 1950-07-01 1950-08-01 1950-09-01 1950-10-01 1950-11-01 1950-12-01",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList",
                doc);
        // check full QuantityList value
        assertXpathEvaluatesTo(
                "16.2 17.1 22.0 25.1 23.9 22.8 17.0 10.2 9.2 7.1 12.3 12.9 17.2 23.6 21.6 21.9 17.6 14.0 9.3 3.8",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList",
                doc);
    }

    protected void checkPointFeatureTwo(Document doc) {
        String id = "ID2";
        // location
        assertXpathEvaluatesTo(
                "42.58 31.29",
                "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:location",
                doc);
        assertXpathEvaluatesTo(
                "epsg:27700",
                "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:location/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "metre metre",
                "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:location/@uomLabels",
                doc);
        assertXpathEvaluatesTo(
                "Easting Northing",
                "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:location/@axisLabels",
                doc);
        // PointSeriesCoverage
        assertXpathCount(1, "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:value", doc);
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage",
                doc);
        assertXpathEvaluatesTo(
                "ID2.1",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/@gml:id",
                doc);
        // TimeSeries
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain",
                doc);
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries",
                doc);
        assertXpathEvaluatesTo(
                "ID2.2",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/@gml:id",
                doc);

        // timePositionList
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList",
                doc);

        // quantityList
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet",
                doc);
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray",
                doc);
        assertXpathEvaluatesTo(
                "ID2.3",
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/@gml:id",
                doc);
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent",
                doc);
        assertXpathCount(
                1,
                "//csml:PointSeriesFeature[@gml:id='"
                        + id
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList",
                doc);

        // parameter xlink:href
        assertXpathEvaluatesTo(
                "http://cf-pcmdi.llnl.gov/documents/cf-standard-names/standard-name-table/current/cf-standard-name-table.html#surface_temperature",
                "//csml:PointSeriesFeature[@gml:id='" + id + "']/csml:parameter/@xlink:href",
                doc);
    }

    /** Test filtering quantity list that is feature chained. */
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
        // subsetting doesn't work with feature chaining, therefore full lists are returned
        assertXpathEvaluatesTo(
                "1949-05-01 1949-06-01 1949-07-01 1949-08-01 1949-09-01 1949-10-01 1949-11-01 1949-12-01 1950-01-01 1950-02-01"
                        + " 1950-03-01 1950-04-01 1950-05-01 1950-06-01 1950-07-01 1950-08-01 1950-09-01 1950-10-01 1950-11-01 1950-12-01",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/csml:pointSeriesDomain/csml:TimeSeries/csml:timePositionList",
                doc);
        assertXpathEvaluatesTo(
                "16.2 17.1 22.0 25.1 23.9 22.8 17.0 10.2 9.2 7.1 12.3 12.9 17.2 23.6 21.6 21.9 17.6 14.0 9.3 3.8",
                "//csml:PointSeriesFeature[@gml:id='"
                        + "ID2"
                        + "']/csml:value/csml:PointSeriesCoverage/gml:rangeSet/gml:ValueArray/gml:valueComponent/gml:QuantityList",
                doc);
    }
}
