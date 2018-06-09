/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS GetFeature to test testing GEOT-4567: using String constants in idExpression with joining.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 */
public class StringConstantIdWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected StringConstantIdMockData createTestData() {
        return new StringConstantIdMockData();
    }

    /** Test whether GetFeature returns wfs:FeatureCollection. */
    @Test
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathEvaluatesTo("string constant", "(//gsml:MappedFeature)[1]/@gml:id", doc);
        // test feature chaining
        assertXpathCount(1, "//gsml:GeologicUnit/@gml:id", doc);
        if (System.getProperty("testDatabase") != null) {
            // when run online, gml:id is prefixed with table name
            assertXpathEvaluatesTo("GEOLOGICUNIT.4", "//gsml:GeologicUnit/@gml:id", doc);
        } else {
            assertXpathEvaluatesTo("4", "//gsml:GeologicUnit/@gml:id", doc);
        }
    }
}
