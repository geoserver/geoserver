/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS test based on Observations and Measurements (om) 2.0
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class Observation_2_0_WfsTest extends AbstractAppSchemaTestSupport {

    /** @see org.geoserver.test.AbstractAppSchemaTestSupport#buildTestData() */
    @Override
    protected Observation_2_0_MockData createTestData() {
        return new Observation_2_0_MockData();
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() {
        String path = "wfs?request=GetFeature&typename=om:OM_Observation&outputFormat=gml32";
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :\n" + prettyString(doc));
        // Cannot validate because the type name contains underscore and invalid!
        // validateGet(path);
        assertXpathCount(2, "//om:OM_Observation", doc);

        String id = "ID1";
        assertXpathEvaluatesTo(id, "(//om:OM_Observation)[1]/@gml:id", doc);
        assertXpathEvaluatesTo(
                "TP." + id,
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:phenomenonTime/gml:TimePeriod/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "1948-01-01T00:00:00Z",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:phenomenonTime/gml:TimePeriod/gml:beginPosition",
                doc);
        assertXpathEvaluatesTo(
                "1949-04-01T00:00:00Z",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:phenomenonTime/gml:TimePeriod/gml:endPosition",
                doc);

        assertXpathEvaluatesTo(
                "measurement." + id,
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "tpl." + id,
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:domainSet/wml2dr:TimePositionList/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "1948-01-01T00:00:00Z 1948-02-01T00:00:00Z 1948-03-01T00:00:00Z 1948-04-01T00:00:00Z 1948-05-01T00:00:00Z "
                        + "1948-06-01T00:00:00Z 1948-07-01T00:00:00Z 1948-08-01T00:00:00Z 1948-09-01T00:00:00Z 1948-10-01T00:00:00Z "
                        + "1948-11-01T00:00:00Z 1948-12-01T00:00:00Z 1949-01-01T00:00:00Z 1949-02-01T00:00:00Z 1949-03-01T00:00:00Z "
                        + "1949-04-01T00:00:00Z",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:domainSet/wml2dr:TimePositionList/wml2dr:timePositionList",
                doc);
        assertXpathEvaluatesTo(
                "missing missing 8.9 7.9 14.2 15.4 18.1 19.1 21.7 20.8 19.6 14.9 10.8 8.8 8.5 10.4",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:rangeSet/gml:QuantityList",
                doc);
        assertXpathEvaluatesTo(
                "degC",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:rangeSet/gml:QuantityList/@uom",
                doc);
        assertXpathEvaluatesTo(
                "string",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:coverageFunction/gml:MappingRule",
                doc);
        assertXpathEvaluatesTo(
                "http://ns.bgs.ac.uk/thesaurus/lithostratigraphy",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gmlcov:rangeType/@xlink:href",
                doc);

        id = "ID2";
        assertXpathEvaluatesTo(id, "(//om:OM_Observation)[2]/@gml:id", doc);
        assertXpathEvaluatesTo(
                "TP." + id,
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:phenomenonTime/gml:TimePeriod/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "1949-05-01T00:00:00Z",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:phenomenonTime/gml:TimePeriod/gml:beginPosition",
                doc);
        assertXpathEvaluatesTo(
                "1950-12-01T00:00:00Z",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:phenomenonTime/gml:TimePeriod/gml:endPosition",
                doc);
        assertXpathEvaluatesTo(
                "measurement." + id,
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "tpl." + id,
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:domainSet/wml2dr:TimePositionList/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "1949-05-01T00:00:00Z 1949-06-01T00:00:00Z 1949-07-01T00:00:00Z 1949-08-01T00:00:00Z 1949-09-01T00:00:00Z "
                        + "1949-10-01T00:00:00Z 1949-11-01T00:00:00Z 1949-12-01T00:00:00Z 1950-01-01T00:00:00Z 1950-02-01T00:00:00Z "
                        + "1950-03-01T00:00:00Z 1950-04-01T00:00:00Z 1950-05-01T00:00:00Z 1950-06-01T00:00:00Z 1950-07-01T00:00:00Z "
                        + "1950-08-01T00:00:00Z 1950-09-01T00:00:00Z 1950-10-01T00:00:00Z 1950-11-01T00:00:00Z 1950-12-01T00:00:00Z",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:domainSet/wml2dr:TimePositionList/wml2dr:timePositionList",
                doc);
        assertXpathEvaluatesTo(
                "16.2 17.1 22.0 25.1 23.9 22.8 17.0 10.2 9.2 7.1 12.3 12.9 17.2 23.6 21.6 21.9 17.6 14.0 9.3 3.8",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:rangeSet/gml:QuantityList",
                doc);
        assertXpathEvaluatesTo(
                "degC",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:rangeSet/gml:QuantityList/@uom",
                doc);
        assertXpathEvaluatesTo(
                "string",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:coverageFunction/gml:MappingRule",
                doc);
        assertXpathEvaluatesTo(
                "http://ns.bgs.ac.uk/thesaurus/lithostratigraphy",
                "//om:OM_Observation[@gml:id='"
                        + id
                        + "']/om:result/wml2dr:MeasurementTimeseriesDomainRange/gmlcov:rangeType/@xlink:href",
                doc);
    }

    /** Test filtering timePositionList expecting a subset. */
    @Test
    public void testTimePositionSubset() {
        String beginPosition = "1950-03-01T00:00:00Z";
        String endPosition = "1950-06-01T00:00:00Z";
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" " //
                        + "version=\"1.1.0\" " //
                        + "outputFormat=\"gml32\" " //
                        + "xmlns:om=\"http://www.opengis.net/om/2.0\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
                        + "xmlns:wml2dr=\"http://www.opengis.net/waterml/DR/2.0\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd" //
                        + "\"" //
                        + ">" //
                        + "<wfs:Query typeName=\"om:OM_Observation\">"
                        + "    <ogc:Filter>"
                        + "        <ogc:PropertyIsBetween>"
                        + "             <ogc:PropertyName>"
                        + " om:OM_Observation/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:domainSet/wml2dr:TimePositionList/wml2dr:timePositionList"
                        + "</ogc:PropertyName>" //
                        + "             <ogc:LowerBoundary><ogc:Literal>"
                        + beginPosition
                        + "</ogc:Literal></ogc:LowerBoundary>" //
                        + "             <ogc:UpperBoundary><ogc:Literal>"
                        + endPosition
                        + "</ogc:Literal></ogc:UpperBoundary>" //
                        + "        </ogc:PropertyIsBetween>"
                        + "    </ogc:Filter>"
                        + "</wfs:Query> "
                        + "</wfs:GetFeature>";
        // Cannot validate because the type name contains underscore and invalid!
        // validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//om:OM_Observation", doc);
        String id = "ID2";
        assertXpathEvaluatesTo(id, "//om:OM_Observation/@gml:id", doc);
        assertXpathEvaluatesTo(
                "TP." + id, "//om:OM_Observation/om:phenomenonTime/gml:TimePeriod/@gml:id", doc);
        // timePeriod should match filter range using index(LAST) in the mapping file
        assertXpathEvaluatesTo(
                beginPosition,
                "//om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:beginPosition",
                doc);
        assertXpathEvaluatesTo(
                endPosition,
                "//om:OM_Observation/om:phenomenonTime/gml:TimePeriod/gml:endPosition",
                doc);
        assertXpathEvaluatesTo(
                "measurement." + id,
                "//om:OM_Observation/om:result/wml2dr:MeasurementTimeseriesDomainRange/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "tpl." + id,
                "//om:OM_Observation/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:domainSet/wml2dr:TimePositionList/@gml:id",
                doc);
        // subset values matching the list (isList requirement)
        assertXpathEvaluatesTo(
                "1950-03-01T00:00:00Z 1950-04-01T00:00:00Z 1950-05-01T00:00:00Z 1950-06-01T00:00:00Z",
                "//om:OM_Observation/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:domainSet/wml2dr:TimePositionList/wml2dr:timePositionList",
                doc);
        // matching QuantityList subset
        assertXpathEvaluatesTo(
                "12.3 12.9 17.2 23.6",
                "//om:OM_Observation/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:rangeSet/gml:QuantityList",
                doc);
        assertXpathEvaluatesTo(
                "degC",
                "//om:OM_Observation/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:rangeSet/gml:QuantityList/@uom",
                doc);
        assertXpathEvaluatesTo(
                "string",
                "//om:OM_Observation/om:result/wml2dr:MeasurementTimeseriesDomainRange/gml:coverageFunction/gml:MappingRule",
                doc);
        assertXpathEvaluatesTo(
                "http://ns.bgs.ac.uk/thesaurus/lithostratigraphy",
                "//om:OM_Observation/om:result/wml2dr:MeasurementTimeseriesDomainRange/gmlcov:rangeType/@xlink:href",
                doc);
    }
}
