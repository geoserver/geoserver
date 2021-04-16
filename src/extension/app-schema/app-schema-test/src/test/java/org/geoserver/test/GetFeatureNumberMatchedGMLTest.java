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
public class GetFeatureNumberMatchedGMLTest extends AbstractAppSchemaTestSupport {

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

        assertNumberMathcedAndNumberReturned(doc, 5, 0);
    }

    /** Test that count with a filter pointing to a root property works */
    @Test
    public void testGetFeatureHitsCountWithFilterOnRootAttribute() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");
        LOGGER.info(prettyString(doc));
        assertNumberMathcedAndNumberReturned(doc, 1, 0);
    }

    /** Test that count with a filter pointing to a nested property works */
    @Test
    public void testGetFeatureHitsCountWithFilterOnNestedAttribute() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'");
        LOGGER.info(prettyString(doc));
        assertNumberMathcedAndNumberReturned(doc, 1, 0);
    }

    /** Test that count with a filter pointing to a nested property works */
    @Test
    public void testGetFeatureHitsCountWithFilterOnNestedAttribute2() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27");
        LOGGER.info(prettyString(doc));

        assertNumberMathcedAndNumberReturned(doc, 3, 0);
    }

    @Test
    public void testGetFeatureHitsCountWithFilterOnNestedAttributeWithMaxNumber() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml3&request=GetFeature&typeNames=gsml:MappedFeature&resulttype=hits"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27&count=1");
        LOGGER.info(prettyString(doc));
        assertNumberMathcedAndNumberReturned(doc, 3, 0);
    }

    /** Test that count with a filter pointing to a nested property works */
    @Test
    public void testGetFeatureNumberMatchedWithFilterOnNestedAttribute() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'");
        LOGGER.info(prettyString(doc));

        assertNumberMathcedAndNumberReturned(doc, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithFilterOnNestedAttribute2() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27");
        LOGGER.info(prettyString(doc));

        assertNumberMathcedAndNumberReturned(doc, 3, 3);
    }

    @Test
    public void testGetFeatureNumberMatchedWithAndNestedFilterOnSameTypes() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'"
                                + "AND gsml:specification.gsml:GeologicUnit.gml:name = 'New Group'");
        assertNumberMathcedAndNumberReturned(doc, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithComplexPropertyORSimpleProperty() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27"
                                + " OR gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        assertNumberMathcedAndNumberReturned(doc, 4, 4);
    }

    @Test
    public void testGetFeatureNumberMatchedWithSimplePropertyANDComplexProperty() throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:description = 'Olivine basalt'"
                                + " AND gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        assertNumberMathcedAndNumberReturned(doc, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithComplexPropertyORSimplePropertyWithPagination()
            throws Exception {

        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27"
                                + " OR gsml:MappedFeature.gml:name = 'MURRADUC BASALT'&startIndex=3&count=2");

        assertNumberMathcedAndNumberReturned(doc, 4, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithMultipleAND() throws Exception {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter=gsml:specification.gsml:GeologicUnit.gml:name = 'New Group'"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%25%27 AND gsml:MappedFeature.gml:name = 'MURRADUC BASALT'");

        assertNumberMathcedAndNumberReturned(doc, 1, 1);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilter() throws Exception {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27");

        assertNumberMathcedAndNumberReturned(doc, 3, 3);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilterWithPagination() throws Exception {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27&startIndex=1");

        assertNumberMathcedAndNumberReturned(doc, 3, 2);
    }

    @Test
    public void testGetFeatureNumberMatchedWithGeomComplexFilterManyAND() throws Exception {
        Document doc =
                getAsDOM(
                        "ows?service=WFS&version=2.0.0&outputFormat=gml32&request=GetFeature&typeNames=gsml:MappedFeature"
                                + "&cql_filter= intersects(gsml:shape, buffer(POLYGON((-1.3 52.5,-1.3 52.6,-1.2 52.6,-1.2 52.5,-1.3 52.5)),100))"
                                + " AND gsml:MappedFeature.gsml:specification.gsml:GeologicUnit.gml:description LIKE %27%25Olivine%20basalt%2C%20tuff%25%27 AND gsml:MappedFeature.gml:name = 'GUNTHORPE FORMATION'");

        assertNumberMathcedAndNumberReturned(doc, 1, 1);
    }

    private void assertNumberMathcedAndNumberReturned(
            Document doc, int numberMatched, int numberReturned) {
        assertXpathEvaluatesTo(
                String.valueOf(numberMatched), "/wfs:FeatureCollection/@numberMatched", doc);
        assertXpathEvaluatesTo(
                String.valueOf(numberReturned), "/wfs:FeatureCollection/@numberReturned", doc);
    }
}
