/*
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import junit.framework.Test;

import org.w3c.dom.Document;

/**
 * Test feature chaining where nested feature has no ID
 * 
 * @author Victor Tey, CSIRO Exploration and Mining
 * 
 */
public class GUChainNoIDMFTest extends AbstractAppSchemaWfsTestSupport {

    /**
     * Read-only test so can use one-time setup.
     * 
     * @return
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GUChainNoIDMFTest());
    }

    @Override
    protected NamespaceTestData buildTestData() {
        return new GUChainNoIDMFTestMockData();
    }

    public void testInLineFeatureNoId() {
        Document doc = getAsDOM("wfs?request=GetFeature&typename=gsml:GeologicUnit");
        LOGGER.info("WFS DescribeFeatureType, typename=gsml:GeologicUnit response:\n"
                + prettyString(doc));
        assertXpathEvaluatesTo("Olivine basalt", "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:occurrence/gsml:MappedFeature/gml:description", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:occurrence/gsml:MappedFeature/gml:description", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence/gsml:MappedFeature/gml:description", doc);
        assertXpathCount(1, "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:occurrence/gsml:MappedFeature/gml:description", doc);

    }
}
