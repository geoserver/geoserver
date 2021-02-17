/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test the proper encoding of duplicated/repeated features with Ids
 *
 * @author Victor Tey, CSIRO Exploration and Mining
 */
public class GetFeaturesNumberMatchedTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32MockData createTestData() {
        return new FeatureGML32MockData();
    }

    /** Tests that a count for All the features works * */
    @Test
    public void testGetMappedFeatureHitsCount() {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits");
        LOGGER.info("WFS GetFeature, typename=gsml:MappedFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberMatched", doc);
    }

    /** Test that count with a filter pointing to a root property works */
    @Test
    public void testGetFeatureHitsCountWithFilterOnRootAttribute() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");
        LOGGER.info(prettyString(doc));

        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberMatched", doc);
    }

    /** Test that count with a filter pointing to a nested property works */
    @Test
    public void testGetFeatureHitsCountWithFilterOnNestedAttribute() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'");
        LOGGER.info(prettyString(doc));

        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberMatched", doc);
    }

    /** Test that count with a filter pointing to a nested property works */
    @Test
    public void testGetFeatureHitsCountWithFilterOnNestedAttribute2() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27");
        LOGGER.info(prettyString(doc));

        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberMatched", doc);
    }

    @Test
    public void testGetFeatureHitsCountWithFilterOnNestedAttributeWithMaxNumber() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27&count=1");
        LOGGER.info(prettyString(doc));

        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberMatched", doc);
    }

    /** Test that count with a filter pointing to a nested property works */
    @Test
    public void testGetFeatureNumberMatchedWithFilterOnNestedAttribute() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'");
        LOGGER.info(prettyString(doc));

        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberMatched", doc);
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberReturned", doc);
    }

    @Test
    public void testGetFeatureNumberMatchedWithFilterOnNestedAttribute2() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27");
        LOGGER.info(prettyString(doc));

        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberMatched", doc);
        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberReturned", doc);
    }
}
