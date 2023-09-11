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
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Function;
import org.geotools.dggs.h3.H3DGGSFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.EnvFunction;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class NeighborFunctionTest {
    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();
    static DGGSInstance DGGS;

    @BeforeClass
    public static void onSetup() throws IOException {
        DGGS = new H3DGGSFactory().createInstance(null);
    }

    @After
    public void cleanupEnv() {
        EnvFunction.clearLocalValues();
    }

    @Test
    public void testNeighborFunctionCacheable() {
        Function testedZone = FF.function("env", FF.literal("test"));

        Function neighbor =
                FF.function(
                        "neighbor",
                        testedZone,
                        FF.literal("807ffffffffffff"),
                        FF.literal(1),
                        FF.literal(DGGS));
        assertNotNull(neighbor);

        // test with a couple neighbors
        EnvFunction.setLocalValue("test", "805bfffffffffff");
        assertEquals(Boolean.TRUE, neighbor.evaluate(null));
        EnvFunction.setLocalValue("test", "809bfffffffffff");
        assertEquals(Boolean.TRUE, neighbor.evaluate(null));

        // test with one that is not
        EnvFunction.setLocalValue("test", "800bfffffffffff");
        assertEquals(Boolean.FALSE, neighbor.evaluate(null));

        // test with one that is not even valid
        EnvFunction.setLocalValue("test", "abcd");
        assertEquals(Boolean.FALSE, neighbor.evaluate(null));

        // test with null
        EnvFunction.setLocalValue("test", null);
        assertEquals(Boolean.FALSE, neighbor.evaluate(null));
    }

    @Test
    public void testNeighborFunctionNonCacheable() {
        Function testedZone = FF.function("env", FF.literal("test"));
        Function referenceZone = FF.function("env", FF.literal("testReference"));

        Function neighbor =
                FF.function("neighbor", testedZone, referenceZone, FF.literal(1), FF.literal(DGGS));
        assertNotNull(neighbor);

        // setup the reference zone
        EnvFunction.setLocalValue("testReference", "807ffffffffffff");

        // test with a couple neighbors
        EnvFunction.setLocalValue("test", "805bfffffffffff");
        assertEquals(Boolean.TRUE, neighbor.evaluate(null));
        EnvFunction.setLocalValue("test", "809bfffffffffff");
        assertEquals(Boolean.TRUE, neighbor.evaluate(null));

        // test with one that is not
        EnvFunction.setLocalValue("test", "800bfffffffffff");
        assertEquals(Boolean.FALSE, neighbor.evaluate(null));

        // test with one that is not even valid
        EnvFunction.setLocalValue("test", "abcd");
        assertEquals(Boolean.FALSE, neighbor.evaluate(null));

        // test with null
        EnvFunction.setLocalValue("test", null);
        assertEquals(Boolean.FALSE, neighbor.evaluate(null));
    }
}
