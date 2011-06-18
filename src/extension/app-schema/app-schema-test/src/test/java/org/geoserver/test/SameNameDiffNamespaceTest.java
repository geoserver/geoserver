/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.w3c.dom.Document;

import junit.framework.Test;

/**
 * Tests whether two 'name' properties with different namespace are properly encoded
 * 
 * @author Niels Charlier, Curtin University of Technology
 * 
 */
public class SameNameDiffNamespaceTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new SameNameDiffNamespaceTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new SameNameDiffNamespaceMockData();
    }
       
    /**
     * Test GetFeature with two 'name' properties with different namespace.
     */
    public void testSameNameDiffNamespace3() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=ex:MyTestFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        assertXpathCount(1, "//ex:MyTestFeature[@gml:id='f1']/gml:name", doc);
        assertXpathCount(1, "//ex:MyTestFeature[@gml:id='f1']/ex:name", doc);
        
    }
    
}
