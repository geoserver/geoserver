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
public class PropertySelectionTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new PropertySelectionTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new PropertySelectionMockData();
    }

    /**
     * Test GetFeature with Property Selection.
     */
    public void testGetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=gsml:MappedFeature&propertyname=description");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        
        // using custom IDs - this is being tested too
        
        //check if requested property is present
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:description", doc);
        
        //check if required property is present
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape", doc);
        
        //check if non-requested property is not present
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:metadata", doc);
        assertXpathCount(0, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name", doc);
        
    }
    
    /**
     * Test GetFeature with Property Selection, using client properties.
     */
    public void testGetFeatureClientProperty() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=gsml:MappedFeature&propertyname=metadata");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        
        //test client property works
        assertXpathEvaluatesTo("zzzgu.25699",  "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:metadata/@xlink:href", doc);
    }
    
    /**
     * Test GetFeature with Property Selection, with an invalid column name.
     */
    public void testGetFeatureInvalidName() {
        Document doc = getAsDOM("wfs?request=GetFeature&typeName=gsml:MappedFeature&propertyname=name");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));
        
        //test exception refering to missing column
        assertTrue(evaluate("//ows:ExceptionText", doc).endsWith("No value for xpath: DOESNT_EXIST"));
        
    }

}
