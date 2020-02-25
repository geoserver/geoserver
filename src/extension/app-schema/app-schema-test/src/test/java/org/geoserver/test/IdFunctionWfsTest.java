/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS GetFeature to test GEOS-5618: using functions in idExpression with joining. If the function
 * is not translatable to SQL, it is not supported with joining. However, it should work without
 * joining.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class IdFunctionWfsTest extends AbstractAppSchemaTestSupport {

    private String mf1;

    private String mf2;

    private String mf3;

    private String mf4;

    public IdFunctionWfsTest() {
        mf1 = "mf1";
        mf2 = "mf2";
        mf3 = "mf3";
        mf4 = "mf4";
        if (System.getProperty("testDatabase") != null) {
            // when run online, getID() is prefixed with table name
            final String PREFIX = "MAPPEDFEATUREPROPERTYFILE.";
            mf1 = PREFIX + mf1;
            mf2 = PREFIX + mf2;
            mf3 = PREFIX + mf3;
            mf4 = PREFIX + mf4;
        }
    }

    @Override
    protected IdFunctionMockData createTestData() {
        return new IdFunctionMockData();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        AppSchemaDataAccessRegistry.getAppSchemaProperties()
                .setProperty("app-schema.joining", "false");
        super.setUpTestData(testData);
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());
    }

    /** Test content of GetFeature response. */
    @Test
    public void testGetFeatureContent() throws Exception {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);

        // mf1
        {
            String id = mf1;
            assertXpathEvaluatesTo(id, "(//gsml:MappedFeature)[1]/@gml:id", doc);
            assertXpathEvaluatesTo(
                    "GUNTHORPE FORMATION",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gml:name",
                    doc);
            // shape
            assertXpathEvaluatesTo(
                    "urn:x-ogc:def:crs:EPSG:4326",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape/gml:Polygon/@srsName",
                    doc);
            assertXpathEvaluatesTo(
                    "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape//gml:posList",
                    doc);
            // specification gu.25699
            assertXpathEvaluatesTo(
                    "gu.25699",
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification/gsml:GeologicUnit/@gml:id",
                    doc);
            // description
            assertXpathEvaluatesTo(
                    "Olivine basalt, tuff, microgabbro, minor sedimentary rocks",
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification"
                            + "/gsml:GeologicUnit/gml:description",
                    doc);
            // name
            assertXpathCount(
                    2,
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification"
                            + "/gsml:GeologicUnit/gml:name",
                    doc);
            assertXpathEvaluatesTo(
                    "Yaugher Volcanic Group",
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification"
                            + "/gsml:GeologicUnit/gml:name[@codeSpace='urn:ietf:rfc:2141']",
                    doc);
            // multi-valued leaf attributes that are feature chained come in random order
            // when joining is used
            List<String> names = new ArrayList<String>();
            names.add("Yaugher Volcanic Group");
            names.add("-Py");
            String name =
                    evaluate(
                            "//gsml:MappedFeature[@gml:id='"
                                    + id
                                    + "']/gsml:specification/gsml:GeologicUnit/gml:name[1]",
                            doc);
            assertTrue(names.contains(name));
            names.remove(name);
            name =
                    evaluate(
                            "//gsml:MappedFeature[@gml:id='"
                                    + id
                                    + "']/gsml:specification/gsml:GeologicUnit/gml:name[2]",
                            doc);
            assertTrue(names.contains(name));
            names.remove(name);
            assertTrue(names.isEmpty());
        }

        // mf2
        {
            String id = mf2;
            assertXpathEvaluatesTo(id, "(//gsml:MappedFeature)[2]/@gml:id", doc);
            assertXpathEvaluatesTo(
                    "MERCIA MUDSTONE GROUP",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gml:name",
                    doc);
            // shape
            assertXpathEvaluatesTo(
                    "urn:x-ogc:def:crs:EPSG:4326",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape/gml:Polygon/@srsName",
                    doc);
            assertXpathEvaluatesTo(
                    "52.5 -1.3 52.6 -1.3 52.6 -1.2 52.5 -1.2 52.5 -1.3",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape//gml:posList",
                    doc);
            // gu.25678
            assertXpathEvaluatesTo(
                    "gu.25678",
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification/gsml:GeologicUnit/@gml:id",
                    doc);
            // name
            assertXpathCount(
                    3,
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification"
                            + "/gsml:GeologicUnit/gml:name",
                    doc);
            // multi-valued leaf attributes that are feature chained come in random order
            // when joining is used
            HashMap<String, String> names = new HashMap<String, String>();
            names.put("Yaugher Volcanic Group 1", "urn:ietf:rfc:2141");
            names.put("Yaugher Volcanic Group 2", "urn:ietf:rfc:2141");
            names.put("-Py", "");
            String name =
                    evaluate(
                            "//gsml:MappedFeature[@gml:id='"
                                    + id
                                    + "']/gsml:specification/gsml:GeologicUnit/gml:name[1]",
                            doc);
            assertTrue(names.containsKey(name));
            assertXpathEvaluatesTo(
                    names.get(name),
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification/gsml:GeologicUnit/gml:name[1]/@codeSpace",
                    doc);
            names.remove(name);

            name =
                    evaluate(
                            "//gsml:MappedFeature[@gml:id='"
                                    + id
                                    + "']/gsml:specification/gsml:GeologicUnit/gml:name[2]",
                            doc);
            assertTrue(names.containsKey(name));
            assertXpathEvaluatesTo(
                    names.get(name),
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification/gsml:GeologicUnit/gml:name[2]/@codeSpace",
                    doc);
            names.remove(name);

            name =
                    evaluate(
                            "//gsml:MappedFeature[@gml:id='"
                                    + id
                                    + "']/gsml:specification/gsml:GeologicUnit/gml:name[3]",
                            doc);
            assertTrue(names.containsKey(name));
            assertXpathEvaluatesTo(
                    names.get(name),
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification/gsml:GeologicUnit/gml:name[3]/@codeSpace",
                    doc);
            names.remove(name);
            assertTrue(names.isEmpty());
        }

        // mf3
        {
            String id = mf3;
            assertXpathEvaluatesTo(id, "(//gsml:MappedFeature)[3]/@gml:id", doc);
            assertXpathEvaluatesTo(
                    "CLIFTON FORMATION",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gml:name",
                    doc);
            // shape
            assertXpathEvaluatesTo(
                    "urn:x-ogc:def:crs:EPSG:4326",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape/gml:Polygon/@srsName",
                    doc);
            assertXpathEvaluatesTo(
                    "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape//gml:posList",
                    doc);
            // gu.25678
            assertXpathEvaluatesTo(
                    "#gu.25678",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:specification/@xlink:href",
                    doc);
        }

        // mf4
        {
            String id = mf4;
            assertXpathEvaluatesTo(id, "(//gsml:MappedFeature)[4]/@gml:id", doc);
            assertXpathEvaluatesTo(
                    "MURRADUC BASALT", "//gsml:MappedFeature[@gml:id='" + id + "']/gml:name", doc);
            // shape
            assertXpathEvaluatesTo(
                    "urn:x-ogc:def:crs:EPSG:4326",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape/gml:Polygon/@srsName",
                    doc);
            assertXpathEvaluatesTo(
                    "52.5 -1.3 52.6 -1.3 52.6 -1.2 52.5 -1.2 52.5 -1.3",
                    "//gsml:MappedFeature[@gml:id='" + id + "']/gsml:shape//gml:posList",
                    doc);
            // gu.25682
            assertXpathEvaluatesTo(
                    "gu.25682",
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification/gsml:GeologicUnit/@gml:id",
                    doc);
            // description
            assertXpathEvaluatesTo(
                    "Olivine basalt",
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification/gsml:GeologicUnit/gml:description",
                    doc);
            // name
            assertXpathCount(
                    2,
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification"
                            + "/gsml:GeologicUnit/gml:name",
                    doc);
            assertXpathEvaluatesTo(
                    "New Group",
                    "//gsml:MappedFeature[@gml:id='"
                            + id
                            + "']/gsml:specification"
                            + "/gsml:GeologicUnit/gml:name[@codeSpace='urn:ietf:rfc:2141']",
                    doc);
            List<String> names = new ArrayList<String>();
            names.add("New Group");
            names.add("-Xy");
            String name =
                    evaluate(
                            "//gsml:MappedFeature[@gml:id='"
                                    + id
                                    + "']/gsml:specification/gsml:GeologicUnit/gml:name[1]",
                            doc);
            assertTrue(names.contains(name));
            names.remove(name);
            name =
                    evaluate(
                            "//gsml:MappedFeature[@gml:id='"
                                    + id
                                    + "']/gsml:specification/gsml:GeologicUnit/gml:name[2]",
                            doc);
            assertTrue(names.contains(name));
            names.remove(name);
            assertTrue(names.isEmpty());
        }
    }
}
