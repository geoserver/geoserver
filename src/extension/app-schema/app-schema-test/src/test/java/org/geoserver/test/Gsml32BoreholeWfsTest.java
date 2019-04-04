/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS test based on GeoSciML 3.2 Borehole type, a GML 3.2 application schema.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class Gsml32BoreholeWfsTest extends AbstractAppSchemaTestSupport {

    /** @see org.geoserver.test.AbstractAppSchemaTestSupport#buildTestData() */
    @Override
    protected Gsml32BoreholeMockData createTestData() {
        return new Gsml32BoreholeMockData();
    }

    /** Test content of GetFeature response. */
    @Test
    public void testGetFeatureContent() throws Exception {
        String path = "wfs?request=GetFeature&typename=gsmlbh:Borehole&outputFormat=gml32";
        String newline = System.getProperty("line.separator");
        Document doc = getAsDOM(path);
        LOGGER.info("Response for " + path + " :" + newline + prettyString(doc));

        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberReturned", doc);
        assertXpathEvaluatesTo("unknown", "/wfs:FeatureCollection/@numberMatched", doc);
        assertXpathCount(2, "//gsmlbh:Borehole", doc);

        // First borehole
        assertXpathCount(1, "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']", doc);
        // Ensure fabricated LineString works as expected
        // Also custom srsName and 1D posList works
        String lineStringPath =
                "sa:relatedSamplingFeature/sa:SamplingFeatureComplex/sa:relatedSamplingFeature/spec:SF_Specimen/spec:samplingLocation/gml:LineString";
        assertXpathEvaluatesTo(
                "borehole.specimen.samplingLocation.GA.100",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "#borehole.shape.GA.17322",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/"
                        + lineStringPath
                        + "/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/@uomLabels",
                doc);
        assertXpathEvaluatesTo(
                "57.9 66.4",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/" + lineStringPath + "/gml:posList",
                doc);
        // test empty Curve
        assertXpathEvaluatesTo(
                "borehole.shape.GA.17322",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/sams:shape/gml:Curve/@gml:id",
                doc);
        assertXpathCount(
                0,
                "/gsmlbh:Borehole[@gml:id='borehole.GA.17322']/sams:shape/gml:Curve/@srsName",
                doc);
        assertXpathCount(
                0,
                "/gsmlbh:Borehole[@gml:id='borehole.GA.17322']/sams:shape/gml:Curve/@srsDimension",
                doc);

        // Second borehole
        assertXpathCount(1, "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']", doc);
        assertXpathEvaluatesTo(
                "borehole.specimen.samplingLocation.GA.102",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "#borehole.shape.GA.17338",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "1",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17322']/"
                        + lineStringPath
                        + "/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "m",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/@uomLabels",
                doc);
        assertXpathEvaluatesTo(
                "85.3 89.6",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/" + lineStringPath + "/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "borehole.shape.GA.17338",
                "//gsmlbh:Borehole[@gml:id='borehole.GA.17338']/sams:shape/gml:Curve/@gml:id",
                doc);
        assertXpathCount(
                0,
                "/gsmlbh:Borehole[@gml:id='borehole.GA.17338']/sams:shape/gml:Curve/@srsName",
                doc);
        assertXpathCount(
                0,
                "/gsmlbh:Borehole[@gml:id='borehole.GA.17338']/sams:shape/gml:Curve/@srsDimension",
                doc);
    }

    // TODO: Reenable after GEOT-4519 is fixed.
    //    /**
    //     * Test filtering fabricated LineString.
    //     */
    //    @Test
    //    public void testFilter() throws Exception {
    //        String xml = "<wfs:GetFeature service=\"WFS\" " //
    //                + "version=\"1.1.0\" " //
    //                + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
    //                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
    //                + "xmlns:gml=\"http://www.opengis.net/gml/3.2\" " //
    //                + "xmlns:gsmlbh=\"http://xmlns.geosciml.org/Borehole/3.2\" " //
    //                + "xmlns:sa=\"http://www.opengis.net/sampling/2.0\" " //
    //                + "xmlns:spec=\"http://www.opengis.net/samplingSpecimen/2.0\" " //
    //                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
    //                + "xsi:schemaLocation=\"" //
    //                + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd "
    // //
    //                + "http://xmlns.geosciml.org/Borehole/3.2
    // http://schemas.geosciml.org/borehole/3.2/borehole.xsd " //
    //                + "http://www.opengis.net/samplingSpecimen/2.0
    // http://schemas.opengis.net/samplingSpecimen/2.0/specimen.xsd" //
    //                + "\">"
    //                + "<wfs:Query typeName=\"gsmlbh:Borehole\">"
    //                + "    <ogc:Filter>"
    //                + "         <ogc:PropertyIsEqualTo>"
    //                + "            <ogc:Literal>85.3 89.6</ogc:Literal>"
    //                + "
    // <ogc:PropertyName>sa:relatedSamplingFeature/sa:SamplingFeatureComplex/sa:relatedSamplingFeature/spec:SF_Specimen/spec:samplingLocation/gml:LineString/gml:posList</ogc:PropertyName>"
    //                + "         </ogc:PropertyIsEqualTo>"
    //                + "    </ogc:Filter>"
    //                + "</wfs:Query> "
    //                + "</wfs:GetFeature>";
    //        validate(xml);
    //        Document doc = postAsDOM("wfs", xml);
    //        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
    //        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
    //        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
    //        assertXpathCount(1, "//gsmlbh:Borehole", doc);
    //        assertXpathEvaluatesTo("borehole.GA.17338", "//gsmlbh:Borehole/@gml:id", doc);
    //    }
}
