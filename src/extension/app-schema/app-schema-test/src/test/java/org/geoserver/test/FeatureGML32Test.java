/*
 * Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.w3c.dom.Document;
import org.junit.Test;

/**
 * 
 * Test the proper encoding of duplicated/repeated features with Ids
 * 
 * @author Victor Tey, CSIRO Exploration and Mining
 */

public class FeatureGML32Test extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32MockData createTestData() {
        return new FeatureGML32MockData();
    }

    @Test
    public void testGetMappedFeature() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&outputFormat=gml32&typename=gsml:MappedFeature");
        LOGGER.info("WFS DescribeFeatureType, typename=gsml:MappedFeature response:\n"
                + prettyString(doc));
        assertXpathEvaluatesTo("#gu.25678",
                "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href", doc);
        assertXpathCount(1, "//gsml:MappedFeature[@gml:id='mf3']/gsml:specification/@xlink:href",
                doc);
    }
}
