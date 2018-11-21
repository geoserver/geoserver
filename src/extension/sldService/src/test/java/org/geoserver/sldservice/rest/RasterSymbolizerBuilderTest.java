/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.sldservice.utils.classifier.RasterSymbolizerBuilder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

public class RasterSymbolizerBuilderTest {

    @Test
    public void testUniqueBinary() throws IOException {
        RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
        RenderedImage image =
                ImageIO.read(
                        RasterSymbolizerBuilderTest.class.getResourceAsStream("milanogeo.tif"));
        ColorMap cm = builder.uniqueIntervalClassification(image, 2);
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(2, entries.length);
        assertLiteralValue(0, entries[0]);
        assertLiteralValue(1, entries[1]);
    }

    @Test
    public void testUniqueBinaryTooManyValues() throws IOException {
        RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
        RenderedImage image =
                ImageIO.read(
                        RasterSymbolizerBuilderTest.class.getResourceAsStream("milanogeo.tif"));
        try {
            builder.uniqueIntervalClassification(image, 1);
            fail("Was expecting an exception");
        } catch (IllegalArgumentException e) {
            assertThat(
                    e.getMessage(),
                    allOf(containsString("2 unique values"), containsString("maximum of 1")));
        }
    }

    @Test
    public void testUniqueDemByte() throws IOException {
        RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
        RenderedImage image =
                ImageIO.read(SystemTestData.class.getResourceAsStream("tazbyte.tiff"));
        ColorMap cm = builder.uniqueIntervalClassification(image, null);
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(168, entries.length);
        assertLiteralValue(0, entries[0]);
        assertLiteralValue(178, entries[entries.length - 1]);
        double prev = 0;
        for (int i = 1; i < entries.length; i++) {
            double curr = entries[i].getQuantity().evaluate(null, Double.class);
            assertTrue(curr > prev);
            prev = curr;
        }
    }

    @Test
    public void testUniqueShortSigned() throws IOException {
        RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
        // short ints, signed
        RenderedImage image = ImageIO.read(SystemTestData.class.getResourceAsStream("hyper.tiff"));
        ColorMap cm = builder.uniqueIntervalClassification(image, null);
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(79, entries.length);
        assertLiteralValue(-6, entries[0]);
        assertLiteralValue(617, entries[entries.length - 1]);
        double prev = Double.NEGATIVE_INFINITY;
        for (int i = 1; i < entries.length; i++) {
            double curr = entries[i].getQuantity().evaluate(null, Double.class);
            assertTrue(curr + " > " + prev, curr > prev);
            prev = curr;
        }
    }

    @Test
    public void testUniqueFloatInvalid() throws IOException {
        RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
        RenderedImage image = ImageIO.read(this.getClass().getResourceAsStream("dem_float.tif"));
        try {
            builder.uniqueIntervalClassification(image, 1);
            fail("Was expecting an exception");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("rasters of float type"));
        }
    }

    @Test
    public void testEqualIntervalContinousFloat() throws IOException {
        RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
        RenderedImage image = ImageIO.read(this.getClass().getResourceAsStream("dem_float.tif"));
        ColorMap cm = builder.equalIntervalClassification(image, 5, false, true);
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        assertLiteralValue(-1, entries[0]);
        assertLiteralValue(311.75, entries[1]);
        assertLiteralValue(624.5, entries[2]);
        assertLiteralValue(937.25, entries[3]);
        assertLiteralValue(1250, entries[4]);
    }

    @Test
    public void testEqualClosedIntervalFloat() throws IOException {
        RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
        RenderedImage image = ImageIO.read(this.getClass().getResourceAsStream("dem_float.tif"));
        ColorMap cm = builder.equalIntervalClassification(image, 5, false, false);
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(6, entries.length);
        assertLiteralValue(-1, entries[0]); // the transparent entry to do closed intervals
        assertLiteralValue(249.2, entries[1]);
        assertLiteralValue(499.4, entries[2]);
        assertLiteralValue(749.6, entries[3]);
        assertLiteralValue(999.8, entries[4]);
        assertLiteralValue(1250, entries[5]);
    }

    @Test
    public void testEqualOpenIntervalFloat() throws IOException {
        RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
        RenderedImage image = ImageIO.read(this.getClass().getResourceAsStream("dem_float.tif"));
        ColorMap cm = builder.equalIntervalClassification(image, 5, true, false);
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(5, entries.length);
        assertLiteralValue(249.2, entries[0]);
        assertLiteralValue(499.4, entries[1]);
        assertLiteralValue(749.6, entries[2]);
        assertLiteralValue(999.8, entries[3]);
        assertLiteralValue(1250, entries[4]);
    }

    @Test
    public void testQuantilesContinous() throws IOException {
        assertOnSRTM(
                image -> {
                    RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
                    ColorMap cm = builder.quantileClassification(image, 5, false, true);
                    ColorMapEntry[] entries = cm.getColorMapEntries();
                    assertEquals(5, entries.length);
                    assertLiteralValue(-2, entries[0]);
                    assertLiteralValue(292, entries[1]);
                    assertLiteralValue(536, entries[2]);
                    assertLiteralValue(825, entries[3]);
                    assertLiteralValue(1796, entries[4]);
                });
    }

    @Test
    public void testQuantilesInterval() throws IOException {
        assertOnSRTM(
                image -> {
                    RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
                    ColorMap cm = builder.quantileClassification(image, 5, false, false);
                    ColorMapEntry[] entries = cm.getColorMapEntries();
                    assertEquals(6, entries.length);
                    assertLiteralValue(-2, entries[0]);
                    assertLiteralValue(237, entries[1]);
                    assertLiteralValue(441, entries[2]);
                    assertLiteralValue(640, entries[3]);
                    assertLiteralValue(894, entries[4]);
                    assertLiteralValue(1796, entries[5]);
                });
    }

    @Test
    public void testJenksContinuous() throws IOException {
        assertOnSRTM(
                image -> {
                    RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
                    ColorMap cm = builder.jenksClassification(image, 5, false, true);
                    ColorMapEntry[] entries = cm.getColorMapEntries();
                    assertEquals(5, entries.length);
                    assertLiteralValue(-2, entries[0]);
                    assertLiteralValue(336, entries[1]);
                    assertLiteralValue(660, entries[2]);
                    assertLiteralValue(1011, entries[3]);
                    assertLiteralValue(1796, entries[4]);
                });
    }

    @Test
    public void testJenksInterval() throws IOException {
        assertOnSRTM(
                image -> {
                    RasterSymbolizerBuilder builder = new RasterSymbolizerBuilder();
                    ColorMap cm = builder.jenksClassification(image, 5, false, false);
                    ColorMapEntry[] entries = cm.getColorMapEntries();
                    assertEquals(6, entries.length);
                    assertLiteralValue(-2, entries[0]);
                    assertLiteralValue(276, entries[1]);
                    assertLiteralValue(531, entries[2]);
                    assertLiteralValue(793, entries[3]);
                    assertLiteralValue(1097, entries[4]);
                    assertLiteralValue(1796, entries[5]);
                });
    }

    private void assertLiteralValue(double value, ColorMapEntry entry) {
        Expression quantity = entry.getQuantity();
        assertThat(quantity, CoreMatchers.instanceOf(Literal.class));
        assertEquals(value, quantity.evaluate(null, Double.class), 1e-6);
    }

    private void assertOnSRTM(Consumer<RenderedImage> performAssert) throws IOException {
        // using a proper DEM_FLOAT with nodata values here
        try (InputStream is = this.getClass().getResourceAsStream("srtm.tif")) {
            GeoTiffReader reader = new GeoTiffReader(is);
            GridCoverage2D coverage = reader.read(null);
            RenderedImage image = coverage.getRenderedImage();
            performAssert.accept(image);
        }
    }
}
