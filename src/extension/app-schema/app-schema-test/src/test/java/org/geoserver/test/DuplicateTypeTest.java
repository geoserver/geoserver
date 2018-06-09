/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test two WFS feature types (XSD elements) with the same XSD type.
 *
 * @author Ben Caradoc-Davies, CSIRO Earth Science and Resource Engineering
 */
public class DuplicateTypeTest extends AbstractAppSchemaTestSupport {

    @Override
    protected DuplicateTypeMockData createTestData() {
        return new DuplicateTypeMockData();
    }

    /** Test GetFeature for gsml:DuplicateMappedFeature. */
    @Test
    public void testGetDuplicateMappedFeature() throws Exception {
        String request = "GetFeature&version=1.1.0&typename=gsml:DuplicateMappedFeature";
        Document doc = getAsDOM("wfs?request=" + request);
        LOGGER.info("WFS " + request + " response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:DuplicateMappedFeature", doc);
        // test that targetAttributeNode works when mapping gsml:positionalAccuracy to
        // gsml:CGI_TermValue
        assertXpathEvaluatesTo(
                "unknown",
                "(//gsml:DuplicateMappedFeature)[1]/gsml:positionalAccuracy/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "",
                "(//gsml:DuplicateMappedFeature)[1]/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
    }

    /** Test GetFeature for gsml:MappedFeature. */
    @Test
    public void testGetMappedFeature() throws Exception {
        String request = "GetFeature&version=1.1.0&typename=gsml:MappedFeature";
        Document doc = getAsDOM("wfs?request=" + request);
        LOGGER.info("WFS " + request + " response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);
        // test that targetAttributeNode works when mapping gsml:positionalAccuracy to
        // gsml:CGI_NumericValue
        assertXpathEvaluatesTo(
                "",
                "(//gsml:MappedFeature)[1]/gsml:positionalAccuracy/gsml:CGI_TermValue/gsml:value",
                doc);
        assertXpathEvaluatesTo(
                "200.0",
                "(//gsml:MappedFeature)[1]/gsml:positionalAccuracy/gsml:CGI_NumericValue/gsml:principalValue",
                doc);
    }
}
