package org.geoserver.filter.function;

import java.util.Collection;

import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;

public class QueryLayerFunctionTest extends GeoServerTestSupport {

    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    public void testQuerySingle() {
        Function function = ff.function("querySingle", // 
                ff.literal(getLayerId(MockData.BUILDINGS)), // 
                ff.literal("ADDRESS"), //
                ff.literal("FID = '113'"));
        
        assertTrue(function instanceof QueryFunction);
        String result = (String) function.evaluate(null);
        assertEquals("123 Main Street", result);
    }
    
    public void testQueryCollection() {
        Function function = ff.function("queryCollection", // 
                ff.literal(getLayerId(MockData.BUILDINGS)), // 
                ff.literal("ADDRESS"), //
                ff.literal("INCLUDE"));
        
        assertTrue(function instanceof QueryFunction);
        Collection result = (Collection) function.evaluate(null);
        assertEquals(2, result.size());
        assertTrue(result.contains("123 Main Street"));
        assertTrue(result.contains("215 Main Street"));
    }
    
    public void testQueryTooMany() throws Exception {
        try {
            // force the reload, otherwise the changed properties won't be noticed
            System.setProperty("QUERY_LAYER_MAX_FEATURES", "3");
            getGeoServer().reload();
            
            Function function = ff.function("queryCollection", // 
                    ff.literal(getLayerId(MockData.ROAD_SEGMENTS)), // 
                    ff.literal("the_geom"), //
                    ff.literal("INCLUDE"));
            
            assertTrue(function instanceof QueryFunction);
            try {
                function.evaluate(null);
                fail("Should have failed with an exception");
            } catch(IllegalStateException e) {
                // fine
            }
        } finally {
            System.clearProperty("QUERY_LAYER_MAX_FEATURES");
        }
    }
}
