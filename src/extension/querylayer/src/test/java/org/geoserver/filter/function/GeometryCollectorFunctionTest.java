package org.geoserver.filter.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geoserver.test.GeoServerTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.io.WKTReader;

public class GeometryCollectorFunctionTest extends GeoServerTestSupport {

    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    public void testCollectNull() {
        Function function = ff.function("collectGeometries", ff.literal(null));
        
        GeometryCollection result = (GeometryCollection) function.evaluate(null);
        assertNotNull(result);
        assertEquals(0, result.getNumGeometries());
    }
    
    public void testCollectNone() {
        Function function = ff.function("collectGeometries", ff.literal(Collections.emptyList()));
        
        GeometryCollection result = (GeometryCollection) function.evaluate(null);
        assertNotNull(result);
        assertEquals(0, result.getNumGeometries());
    }
    
    public void testTwo() throws Exception {
        WKTReader reader = new WKTReader();
        List<Geometry> geometries = new ArrayList<Geometry>();
        final Geometry p0 = reader.read("POINT(0 0)");
        geometries.add(p0);
        final Geometry p1 = reader.read("POINT(1 1)");
        geometries.add(p1);
        
        Function function = ff.function("collectGeometries", ff.literal(geometries));
        GeometryCollection result = (GeometryCollection) function.evaluate(null);
        assertEquals(2, result.getNumGeometries());
        assertSame(p0, result.getGeometryN(0));
        assertSame(p1, result.getGeometryN(1));
    }
    
}
