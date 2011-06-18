/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.w3c.dom.Document;

import junit.framework.Test;

/**
 * Tests whether app-schema can walk through substitution groups automatically,
 * without having to explicitly put a target
 * 
 * @author Niels Charlier, Curtin University of Technology
 * 
 */
public class SubstitutionGroupTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new SubstitutionGroupTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new SubstitutionGroupMockData();
    }

    /**
     * Test GetFeature .
     */
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=gsml:MappedFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("200.0",
                "//gsml:MappedFeature[@gml:id='mf1']/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue", doc);
    }

}
