/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/** @author Niels Charlier (Curtin University of Technology) */
public class IdNotEncodedTest extends AbstractAppSchemaTestSupport {

    @Override
    protected IdNotEncodedMockData createTestData() {
        return new IdNotEncodedMockData();
    }

    /** Test whether GetFeature */
    @Test
    public void testGetFeature() {
        Document doc =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedInterval");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo(
                "ubspatial.hydrostratigraphicunit.123",
                "//gsml:MappedInterval[@gml:id='123']/gsml:specification/gwml:HydrostratigraphicUnit/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "ubspatial.hydrostratigraphicunit.456",
                "//gsml:MappedInterval[@gml:id='456']/gsml:specification/gwml:HydrostratigraphicUnit/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "ubspatial.hydrostratigraphicunit.789",
                "//gsml:MappedInterval[@gml:id='789']/gsml:specification/gwml:HydrostratigraphicUnit/@gml:id",
                doc);
        assertXpathEvaluatesTo(
                "ubspatial.hydrostratigraphicunit.012",
                "//gsml:MappedInterval[@gml:id='012']/gsml:specification/gwml:HydrostratigraphicUnit/@gml:id",
                doc);
    }
}
