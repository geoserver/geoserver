/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.w3c.dom.Document;

import junit.framework.Test;

/**
 * Tests whether we get an exception thrown when an invalid column name is used
 * 
 * @author Niels Charlier, Curtin University of Technology
 * 
 */
public class InvalidColumnTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new InvalidColumnTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new InvalidColumnTestData();
    }

    /**
     * Test whether GetFeature returns ows:ExceptionReport.
     */
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=gsml:GeologicUnit");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//ows:ExceptionReport", doc);
    }

}
