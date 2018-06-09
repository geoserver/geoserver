/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2009 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test GetPropertyValue request, combined with local resolves
 *
 * <p>!! THIS TEST IS ONLY TO BE RUN ONLINE Property Files don't support sorting.
 *
 * @author Niels Charlier
 */
public class SortByTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32MockData createTestData() {
        return new FeatureGML32MockData();
    }

    @Test
    public void testGetMappedFeature() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=2.0&outputFormat=gml32&typename=gsml:MappedFeature&sortBy=gml:name");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));

        // check order
        assertXpathEvaluatesTo(
                "mf3", "//wfs:FeatureCollection/wfs:member[1]/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo(
                "mf1", "//wfs:FeatureCollection/wfs:member[2]/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo(
                "mf2", "//wfs:FeatureCollection/wfs:member[3]/gsml:MappedFeature/@gml:id", doc);
        assertXpathEvaluatesTo(
                "mf4", "//wfs:FeatureCollection/wfs:member[4]/gsml:MappedFeature/@gml:id", doc);

        // check proper chaining
        assertXpathEvaluatesTo(
                "gu.25678",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "gu.25682",
                "//gsml:MappedFeature[@gml:id='mf4']/gsml:specification/gsml:GeologicUnit/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "#gu.25678",
                "//gsml:MappedFeature[@gml:id='mf2']/gsml:specification/@xlink:href",
                doc);
    }
}
