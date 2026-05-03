/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import org.junit.Test;

/**
 * Unit tests for the no-data sentinel predicate in {@link AbstractNetCDFEncoder}.
 *
 * <p>Pins down the regression where a {@code BAND_SELECT} {@code CoverageView} on a float source whose fill cells are
 * NaN ends up leaking {@code -Float.MAX_VALUE} (or {@code Float.MAX_VALUE}) into the output, because JAI /
 * EclipseImagen's {@code BandMerge} substitutes those for NaN when stitching float bands. The encoder must treat those
 * extremes as fill so they are replaced by NaN (matching the {@code _FillValue} attribute) at write time.
 */
public class AbstractNetCDFEncoderTest {

    /** Reflective dispatch — keeps the predicate package-private without forcing it to be {@code public}. */
    private static boolean isNaN(Number sample, double noDataValue) {
        try {
            Method m = AbstractNetCDFEncoder.class.getDeclaredMethod("isNaN", Number.class, double.class);
            m.setAccessible(true);
            return (boolean) m.invoke(null, sample, noDataValue);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to dispatch AbstractNetCDFEncoder.isNaN", e);
        }
    }

    /** When the configured no-data is NaN, real NaN samples must still be flagged as fill. */
    @Test
    public void nanSampleIsFillWhenNoDataIsNaN() {
        assertTrue(isNaN(Double.NaN, Double.NaN));
        assertTrue(isNaN(Float.NaN, Double.NaN));
    }

    /**
     * When the configured no-data is NaN, JAI's {@code -Float.MAX_VALUE} / {@code Float.MAX_VALUE} extremes must be
     * flagged as fill — this is the scenario that motivated the fix.
     */
    @Test
    public void floatExtremesAreFillWhenNoDataIsNaN() {
        assertTrue(isNaN(-Float.MAX_VALUE, Double.NaN));
        assertTrue(isNaN(Float.MAX_VALUE, Double.NaN));
        assertTrue(isNaN(-Double.MAX_VALUE, Double.NaN));
        assertTrue(isNaN(Double.MAX_VALUE, Double.NaN));
    }

    /** Realistic geophysical samples must remain valid even when the configured no-data is NaN. */
    @Test
    public void realSamplesAreNotFillWhenNoDataIsNaN() {
        assertFalse(isNaN(0.5f, Double.NaN));
        assertFalse(isNaN(-273.15, Double.NaN));
        assertFalse(isNaN(1e29, Double.NaN));
        assertFalse(isNaN(-1e29, Double.NaN));
    }

    /** An explicit no-data sentinel matches only samples within the equality delta of it. */
    @Test
    public void explicitSentinelMatchesEqualSamplesOnly() {
        double sentinel = -9999.0;
        assertTrue(isNaN(-9999.0, sentinel));
        assertTrue(isNaN(-9999.0f, sentinel));
        assertFalse(isNaN(0.0, sentinel));
        assertFalse(isNaN(-9998.0, sentinel));
        // With an explicit sentinel, the JAI-extremes guard is irrelevant.
        assertFalse(isNaN(-Float.MAX_VALUE, sentinel));
    }
}
