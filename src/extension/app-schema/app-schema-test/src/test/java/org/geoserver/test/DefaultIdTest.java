/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests whether not specifying idExpression (using primary keys by default) works.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class DefaultIdTest extends AbstractAppSchemaTestSupport {
    private static String ID_PREFIX;

    @Override
    protected DefaultIdMockData createTestData() {

        // generated id prefix depends on the backend
        ID_PREFIX = "";
        if (System.getProperty("testDatabase") != null) {
            // when run online, gml:id is prefixed with table name
            ID_PREFIX = "MAPPEDFEATURENOID.";
        }

        return new DefaultIdMockData();
    }

    /** Test GetFeature. */
    @Test
    public void testGetFeature() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typeName=gsml:MappedFeature Response:\n"
                        + prettyString(doc));
        assertXpathCount(4, "//gsml:MappedFeature", doc);
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='" + ID_PREFIX + "1']", doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + ID_PREFIX
                        + "1']/gsml:specification/gsml:GeologicUnit",
                doc);
        assertXpathEvaluatesTo(
                "gu.25699",
                "//gsml:MappedFeature[@gml:id='"
                        + ID_PREFIX
                        + "1']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);

        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='" + ID_PREFIX + "2']", doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + ID_PREFIX
                        + "2']/gsml:specification/gsml:GeologicUnit",
                doc);
        assertXpathEvaluatesTo(
                "gu.25678",
                "//gsml:MappedFeature[@gml:id='"
                        + ID_PREFIX
                        + "2']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);

        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='" + ID_PREFIX + "3']", doc);
        assertXpathCount(
                1, "//gsml:MappedFeature[@gml:id='" + ID_PREFIX + "3']/gsml:specification", doc);
        assertXpathEvaluatesTo(
                "#gu.25678",
                "//gsml:MappedFeature[@gml:id='" + ID_PREFIX + "3']/gsml:specification/@xlink:href",
                doc);

        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='" + ID_PREFIX + "4']", doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + ID_PREFIX
                        + "4']/gsml:specification/gsml:GeologicUnit",
                doc);
        assertXpathEvaluatesTo(
                "gu.25682",
                "//gsml:MappedFeature[@gml:id='"
                        + ID_PREFIX
                        + "4']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
    }

    /** Test GetFeature with filters. */
    @Test
    public void testGetFeatureWithFilter() {
        Document doc =
                getAsDOM(
                        "wfs?service=WFS&version=1.1.0&request=GetFeature&typename=gsml:MappedFeature&BBOX=-35,96,-12,118");
        LOGGER.info(
                "wfs?service=WFS&version=1.1.0&request=GetFeature&typename=gsml:MappedFeature&BBOX=-35,96,-12,118 response:\n"
                        + prettyString(doc));

        assertXpathCount(2, "//gsml:MappedFeature", doc);

        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='" + ID_PREFIX + "3']", doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + ID_PREFIX
                        + "3']/gsml:specification/gsml:GeologicUnit",
                doc);
        assertXpathEvaluatesTo(
                "gu.25678",
                "//gsml:MappedFeature[@gml:id='"
                        + ID_PREFIX
                        + "3']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);

        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='" + ID_PREFIX + "4']", doc);
        assertXpathCount(
                1,
                "//gsml:MappedFeature[@gml:id='"
                        + ID_PREFIX
                        + "4']/gsml:specification/gsml:GeologicUnit",
                doc);
        assertXpathEvaluatesTo(
                "gu.25682",
                "//gsml:MappedFeature[@gml:id='"
                        + ID_PREFIX
                        + "4']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
    }
}
