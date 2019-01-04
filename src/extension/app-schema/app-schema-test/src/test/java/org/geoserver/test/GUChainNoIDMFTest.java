/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test feature chaining where nested feature has no ID
 *
 * @author Victor Tey, CSIRO Exploration and Mining
 */
public class GUChainNoIDMFTest extends AbstractAppSchemaTestSupport {

    @Override
    protected GUChainNoIDMFTestMockData createTestData() {
        return new GUChainNoIDMFTestMockData();
    }

    @Test
    public void testInLineFeatureNoId() {
        Document doc = getAsDOM("wfs?request=GetFeature&typename=gsml:GeologicUnit&version=1.1.0");
        LOGGER.info(
                "WFS DescribeFeatureType, typename=gsml:GeologicUnit response:\n"
                        + prettyString(doc));
        assertXpathEvaluatesTo(
                "Olivine basalt",
                "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:occurrence/gsml:MappedFeature/gml:description",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gu.25699']/gsml:occurrence/gsml:MappedFeature/gml:description",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence/gsml:MappedFeature/gml:description",
                doc);
        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gu.25682']/gsml:occurrence/gsml:MappedFeature/gml:description",
                doc);
    }
}
