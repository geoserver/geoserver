/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class PagingTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32MockData createTestData() {
        return new FeatureGML32MockData();
    }

    
    @Test
    public void testWfs110GetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&outputFormat=gml32&maxFeatures=2&startIndex=2");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        // expecting mf3 and mf4
        assertXpathCount(2, "//gsml:MappedFeature", doc);
        
        // ensure order is correct too
        Node firstNode = doc.getElementsByTagName("gsml:MappedFeature").item(0);
        assertEquals("mf3", firstNode.getAttributes().getNamedItem("gml:id").getNodeValue());
        
        Node secondNode = doc.getElementsByTagName("gsml:MappedFeature").item(1);
        assertEquals("mf4", secondNode.getAttributes().getNamedItem("gml:id").getNodeValue());
    }
    
    @Test
    public void testWfs200GetFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=2.0.0&typename=gsml:MappedFeature&count=1&startIndex=1");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));
        // expecting mf2
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        assertXpathEvaluatesTo("mf2", "//gsml:MappedFeature/@gml:id", doc);
    }
    
    @Test
    public void testGetFeatureDenormalised() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=2.0.0&typename=gsml:GeologicUnit&count=1&startIndex=1");
        LOGGER.info("WFS GetFeature&typename=gsml:GeologicUnit response:\n" + prettyString(doc));
        // expecting gu.25682
        assertXpathCount(1, "//gsml:GeologicUnit", doc);
        assertXpathEvaluatesTo("gu.25682", "//gsml:GeologicUnit/@gml:id", doc);
    }
    // test sortby
    
    // test bbox
    
    // test sortby, bbox
    
    // test filters
    
    // test CSV
    
    // test WMS
    
    // startIndex 0 should give full results, sorted by natural order (pkey)
    
    // startIndex n would shift the results (starting from n+1)
    
    // no startindex -> no sorting
    
    // test normalised and denormalised

}
