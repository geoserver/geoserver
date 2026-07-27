/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    /** When the configured no-data is NaN, real NaN samples must still be flagged as fill. */
    @Test
    public void nanSampleIsFillWhenNoDataIsNaN() {
        assertTrue(AbstractNetCDFEncoder.isNaN(Double.NaN, Double.NaN));
        assertTrue(AbstractNetCDFEncoder.isNaN(Float.NaN, Double.NaN));
    }

    /**
     * When the configured no-data is NaN, JAI's {@code -Float.MAX_VALUE} / {@code Float.MAX_VALUE} extremes must be
     * flagged as fill — this is the scenario that motivated the fix.
     */
    @Test
    public void floatExtremesAreFillWhenNoDataIsNaN() {
        assertTrue(AbstractNetCDFEncoder.isNaN(-Float.MAX_VALUE, Double.NaN));
        assertTrue(AbstractNetCDFEncoder.isNaN(Float.MAX_VALUE, Double.NaN));
        assertTrue(AbstractNetCDFEncoder.isNaN(-Double.MAX_VALUE, Double.NaN));
        assertTrue(AbstractNetCDFEncoder.isNaN(Double.MAX_VALUE, Double.NaN));
    }

    /** Realistic geophysical samples must remain valid even when the configured no-data is NaN. */
    @Test
    public void realSamplesAreNotFillWhenNoDataIsNaN() {
        assertFalse(AbstractNetCDFEncoder.isNaN(0.5f, Double.NaN));
        assertFalse(AbstractNetCDFEncoder.isNaN(-273.15, Double.NaN));
        assertFalse(AbstractNetCDFEncoder.isNaN(1e29, Double.NaN));
        assertFalse(AbstractNetCDFEncoder.isNaN(-1e29, Double.NaN));
    }

    /** An explicit no-data sentinel matches only samples within the equality delta of it. */
    @Test
    public void explicitSentinelMatchesEqualSamplesOnly() {
        double sentinel = -9999.0;
        assertTrue(AbstractNetCDFEncoder.isNaN(-9999.0, sentinel));
        assertTrue(AbstractNetCDFEncoder.isNaN(-9999.0f, sentinel));
        assertFalse(AbstractNetCDFEncoder.isNaN(0.0, sentinel));
        assertFalse(AbstractNetCDFEncoder.isNaN(-9998.0, sentinel));
        // With an explicit sentinel, the JAI-extremes guard is irrelevant.
        assertFalse(AbstractNetCDFEncoder.isNaN(-Float.MAX_VALUE, sentinel));
    }
}
