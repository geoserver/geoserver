/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filter.function;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;

public class GeometryCollectorFunctionTest extends GeoServerSystemTestSupport {

    static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    @Test
    public void testCollectNull() {
        Function function = ff.function("collectGeometries", ff.literal(null));

        GeometryCollection result = (GeometryCollection) function.evaluate(null);
        assertNotNull(result);
        assertEquals(0, result.getNumGeometries());
    }

    @Test
    public void testCollectNone() {
        Function function = ff.function("collectGeometries", ff.literal(Collections.emptyList()));

        GeometryCollection result = (GeometryCollection) function.evaluate(null);
        assertNotNull(result);
        assertEquals(0, result.getNumGeometries());
    }

    @Test
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
