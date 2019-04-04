/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests whether app-schema can walk through substitution groups automatically, without having to
 * explicitly put a target
 *
 * @author Niels Charlier, Curtin University of Technology
 */
public class SubstitutionGroupTest extends AbstractAppSchemaTestSupport {

    @Override
    protected SubstitutionGroupMockData createTestData() {
        return new SubstitutionGroupMockData();
    }

    /** Test GetFeature . */
    @Test
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo(
                "200.0",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
    }
}
