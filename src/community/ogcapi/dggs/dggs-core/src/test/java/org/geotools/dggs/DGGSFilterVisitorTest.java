/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import org.geotools.dggs.h3.H3DGGSFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;

public class DGGSFilterVisitorTest {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    static DGGSInstance DGGS;

    @BeforeClass
    public static void onSetup() throws IOException {
        DGGS = new H3DGGSFactory().createInstance(null);
    }

    @Test
    public void testNonDGGSFunction() {
        Function random = FF.function("random");
        Function clone = (Function) random.accept(new DGGSFilterVisitor(DGGS), null);
        assertEquals(clone, random);
    }

    @Test
    public void testNeighbor() {
        Function neighbor =
                FF.function(
                        "neighbor",
                        FF.literal("805bfffffffffff"),
                        FF.literal("807ffffffffffff"),
                        FF.literal(1));
        assertNotNull(neighbor);
        Function clone = (Function) neighbor.accept(new DGGSFilterVisitor(DGGS), null);
        assertNotEquals(clone, neighbor);
        assertEquals(DGGS, clone.getParameters().get(3).evaluate(null));
    }
}
