/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2009 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geotools.data.complex.AppSchemaDataAccess;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * WFS GetFeature to test integration of {@link AppSchemaDataAccess} with GeoServer.
 *
 * @author Niels Charlier
 */
public class LocalResolveFeatureChainingByRefTest extends AbstractAppSchemaTestSupport {

    @Override
    protected FeatureGML32ResolveMockData createTestData() {
        return new FeatureGML32ResolveMockData();
    }

    /** Test Local Resolve with Depth 2. */
    @Test
    public void testResolveDepth2() {

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=2.0.0&typename=gsml:GeologicUnit&resolve=local&resolveDepth=2");

        LOGGER.info("WFS testResolveDepth2 response:\n" + prettyString(doc));

        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence/gsml:MappedFeature[@gml:id='mf2']",
                doc);
        assertXpathEvaluatesTo(
                "#gu.25678",
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence/gsml:MappedFeature[@gml:id='mf2']/gsml:specification/@xlink:href",
                doc);
    }

    /** Test Local Resolve with Depth 1. */
    @Test
    public void testResolveDepth1() {

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=2.0.0&typename=gsml:GeologicUnit&resolve=local&resolveDepth=1");

        LOGGER.info("WFS testResolveDepth1 response:\n" + prettyString(doc));

        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence/gsml:MappedFeature[@gml:id='mf2']",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-test:GeologicUnit:gu.25678",
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence/gsml:MappedFeature[@gml:id='mf2']/gsml:specification/@xlink:href",
                doc);
    }

    /** Test Local Resolve with Depth 0. */
    @Test
    public void testResolveDepth0() {

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=2.0.0&typename=gsml:GeologicUnit&resolve=local&resolveDepth=0");

        LOGGER.info("WFS testResolveDepth0 response:\n" + prettyString(doc));

        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence[@xlink:href='urn:cgi:feature:MappedFeature:mf2']",
                doc);
    }

    /** Test Local Resolve is not applied when turned off */
    @Test
    public void testNoResolve() {

        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=2.0.0&typename=gsml:GeologicUnit&resolve=none");

        LOGGER.info("WFS testNoResolve response:\n" + prettyString(doc));

        assertXpathCount(
                1,
                "//gsml:GeologicUnit[@gml:id='gu.25678']/gsml:occurrence[@xlink:href='urn:cgi:feature:MappedFeature:mf2']",
                doc);
    }
}
