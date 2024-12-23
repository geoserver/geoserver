/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2024, Open Source Geospatial Foundation (OSGeo)
 *
 *    This file is hereby placed into the Public Domain. This means anyone is
 *    free to do whatever they wish with this file. Use it well and enjoy!
 */
package org.geotools.dggs.gstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.geotools.api.data.Query;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.h3.H3DGGSFactory;
import org.geotools.filter.function.EnvFunction;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;

public class DGGSResolutionCalculatorTest {

    private DGGSInstance dggsInstance;
    private DGGSResolutionCalculator calculator;

    @Before
    public void setUp() throws IOException {
        dggsInstance = new H3DGGSFactory().createInstance(new HashMap<>());
        calculator = new DGGSResolutionCalculator(dggsInstance);
    }

    @Test
    public void testConstructor() {
        assertNotNull(calculator);
        assertEquals(16, calculator.levelThresholds.length);
    }

    @Test
    public void testIsValid() {
        for (int level = 0; level < 16; level++) {
            assertTrue(calculator.isValid(level));
        }
        assertFalse(calculator.isValid(16));
    }

    @Test
    public void testGetValidResolutions() {
        NumberRange<Integer> range = calculator.getValidResolutions();
        assertEquals(0, (int) range.getMinimum());
        assertEquals(15, (int) range.getMaximum());
    }

    @Test
    public void testGetTargetResolutionDistanceLarge() {
        Query query = new Query("testLayer");
        Hints hints = new Hints();
        hints.put(Hints.GEOMETRY_DISTANCE, 10d); // degrees
        query.setHints(hints);

        int resolution = calculator.getTargetResolution(query, 1);
        assertEquals(0, resolution);
    }

    @Test
    public void testGetResolutionMin() {
        Query query = new Query("testLayer");
        Hints hints = new Hints();
        hints.put(Hints.GEOMETRY_DISTANCE, 10d); // 10 degrees
        hints.put(DGGSResolutionCalculator.MINRES_HINTS_KEY, 5); // min resolution is 5
        query.setHints(hints);

        int resolution = calculator.getTargetResolution(query, 1);
        assertEquals(5, resolution);
    }

    /** When the minimim resolution comes from the GeoServer configuration, it might be a string */
    @Test
    public void testGetResolutionMinString() {
        Query query = new Query("testLayer");
        Hints hints = new Hints();
        hints.put(Hints.GEOMETRY_DISTANCE, 10d); // 10 degrees
        hints.put(DGGSResolutionCalculator.MINRES_HINTS_KEY, String.valueOf("5")); // min resolution is 5
        query.setHints(hints);

        int resolution = calculator.getTargetResolution(query, 1);
        assertEquals(5, resolution);
    }

    @Test
    public void testGetTargetResolutionDistanceSmall() {
        Query query = new Query("testLayer");
        Hints hints = new Hints();
        hints.put(Hints.GEOMETRY_DISTANCE, 0.001d); // degrees
        query.setHints(hints);

        int resolution = calculator.getTargetResolution(query, 1);
        assertEquals(5, resolution);
    }

    @Test
    public void testGetResolutionMax() {
        Query query = new Query("testLayer");
        Hints hints = new Hints();
        hints.put(Hints.GEOMETRY_DISTANCE, 0.001); // in degrees
        hints.put(DGGSResolutionCalculator.MAXRES_HINTS_KEY, 2);
        query.setHints(hints);

        int resolution = calculator.getTargetResolution(query, 1);
        assertEquals(2, resolution);
    }

    @Test
    public void testGetResolutionMaxString() {
        Query query = new Query("testLayer");
        Hints hints = new Hints();
        hints.put(Hints.GEOMETRY_DISTANCE, 0.001); // in degrees
        hints.put(DGGSResolutionCalculator.MAXRES_HINTS_KEY, String.valueOf(2));
        query.setHints(hints);

        int resolution = calculator.getTargetResolution(query, 1);
        assertEquals(2, resolution);
    }

    @Test
    public void testGetTargetResolutionDistanceOffset() {
        Query query = new Query("testLayer");
        Hints hints = new Hints();
        hints.put(Hints.GEOMETRY_DISTANCE, 10d); // 10 degrees
        hints.put(DGGSResolutionCalculator.OFFSET_HINTS_KEY, 1);
        query.setHints(hints);

        int resolution = calculator.getTargetResolution(query, 1);
        assertEquals(1, resolution);
    }

    @Test
    public void testGetTargetResolutionDistanceOffsetString() {
        Query query = new Query("testLayer");
        Hints hints = new Hints();
        hints.put(Hints.GEOMETRY_DISTANCE, 10d); // 10 degrees
        hints.put(DGGSResolutionCalculator.OFFSET_HINTS_KEY, String.valueOf("1"));
        query.setHints(hints);

        int resolution = calculator.getTargetResolution(query, 1);
        assertEquals(1, resolution);
    }

    @Test
    public void testGetTargetResolutionString() {
        Query query = new Query("testLayer");
        Hints hints = new Hints();
        hints.put(Hints.VIRTUAL_TABLE_PARAMETERS, Map.of(DGGSStore.VP_RESOLUTION, String.valueOf(5)));
        query.setHints(hints);

        int resolution = calculator.getTargetResolution(query, 1);
        assertEquals(5, resolution);
    }

    @Test
    public void testGetTargetResolutionWMSScale() {
        EnvFunction.setLocalValues(Map.of(DGGSResolutionCalculator.WMS_SCALE_DENOMINATOR, 10_000_000));
        try {
            Query query = new Query("testLayer");
            int resolution = calculator.getTargetResolution(query, 1);
            assertEquals(2, resolution);
        } finally {
            EnvFunction.clearLocalValues();
        }
    }

    /**
     * This one should not actually happen, but just to be on the safe side, make sure code works even if the scale
     * denominator is a string
     */
    @Test
    public void testGetTargetResolutionWMSScaleString() {
        EnvFunction.setLocalValues(Map.of(DGGSResolutionCalculator.WMS_SCALE_DENOMINATOR, String.valueOf(10_000_000)));
        try {
            Query query = new Query("testLayer");
            int resolution = calculator.getTargetResolution(query, 1);
            assertEquals(2, resolution);
        } finally {
            EnvFunction.clearLocalValues();
        }
    }
}
