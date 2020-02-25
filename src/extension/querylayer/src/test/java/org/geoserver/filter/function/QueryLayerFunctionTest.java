/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filter.function;

import static org.junit.Assert.*;

import java.util.Collection;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;

public class QueryLayerFunctionTest extends GeoServerSystemTestSupport {

    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    @Test
    public void testQuerySingle() {
        Function function =
                ff.function(
                        "querySingle", //
                        ff.literal(getLayerId(MockData.BUILDINGS)), //
                        ff.literal("ADDRESS"), //
                        ff.literal("FID = '113'"));

        assertTrue(function instanceof QueryFunction);
        String result = (String) function.evaluate(null);
        assertEquals("123 Main Street", result);
    }

    @Test
    public void testQueryCollection() {
        Function function =
                ff.function(
                        "queryCollection", //
                        ff.literal(getLayerId(MockData.BUILDINGS)), //
                        ff.literal("ADDRESS"), //
                        ff.literal("INCLUDE"));

        assertTrue(function instanceof QueryFunction);
        Collection result = (Collection) function.evaluate(null);
        assertEquals(2, result.size());
        assertTrue(result.contains("123 Main Street"));
        assertTrue(result.contains("215 Main Street"));
    }

    @Test
    public void testQueryTooMany() throws Exception {
        try {
            // force the reload, otherwise the changed properties won't be noticed
            System.setProperty("QUERY_LAYER_MAX_FEATURES", "3");
            getGeoServer().reload();

            Function function =
                    ff.function(
                            "queryCollection", //
                            ff.literal(getLayerId(MockData.ROAD_SEGMENTS)), //
                            ff.literal("the_geom"), //
                            ff.literal("INCLUDE"));

            assertTrue(function instanceof QueryFunction);
            try {
                function.evaluate(null);
                fail("Should have failed with an exception");
            } catch (IllegalStateException e) {
                // fine
            }
        } finally {
            System.clearProperty("QUERY_LAYER_MAX_FEATURES");
        }
    }
}
