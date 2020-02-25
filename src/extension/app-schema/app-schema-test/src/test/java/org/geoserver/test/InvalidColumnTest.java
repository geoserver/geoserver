/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests whether we get an exception thrown when an invalid column name is used
 *
 * @author Niels Charlier, Curtin University of Technology
 */
public class InvalidColumnTest extends AbstractAppSchemaTestSupport {

    @Override
    protected InvalidColumnTestData createTestData() {
        return new InvalidColumnTestData();
    }

    /** Test whether GetFeature returns ows:ExceptionReport. */
    @Test
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typeName=gsml:GeologicUnit");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//ows:ExceptionReport", doc);
    }
}
