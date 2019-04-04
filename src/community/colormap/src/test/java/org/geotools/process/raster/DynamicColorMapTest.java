/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster;

import static org.geotools.process.raster.FilterFunction_svgColorMap.MAX_PALETTE_COLORS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import it.geosolutions.imageio.utilities.ImageIOUtilities;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.util.NumberRange;
import org.geotools.xml.styling.SLDTransformer;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;

public class DynamicColorMapTest extends GeoServerSystemTestSupport {

    private static final String COVERAGE_NAME = "watertemp_dynamic";

    private static final double TOLERANCE = 0.01;

    GetMapKvpRequestReader requestReader;

    protected static XpathEngine xp;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();

        testData.addStyle(
                "style_rgb", "test-data/style_rgb.sld", DynamicColorMapTest.class, catalog);
        Map<LayerProperty, Object> properties = new HashMap<>();
        properties.put(LayerProperty.STYLE, "style_rgb");
        testData.addRasterLayer(
                new QName(MockData.DEFAULT_URI, "watertemp_dynamic", MockData.DEFAULT_PREFIX),
                "test-data/watertemp_dynamic.zip",
                null,
                properties,
                DynamicColorMapTest.class,
                catalog);

        // setup manual statistics
        CoverageInfo coverage = getCatalog().getCoverageByName("watertemp_dynamic");
        CoverageDimensionInfo di = coverage.getDimensions().get(0);
        di.setRange(new NumberRange<Double>(Double.class, 0., 0.5));
        getCatalog().save(coverage);
    }

    @Test
    public void testGridCoverageStats() throws Exception {

        // check the coverage is actually there
        Catalog catalog = getCatalog();
        CoverageStoreInfo storeInfo = catalog.getCoverageStoreByName(COVERAGE_NAME);
        assertNotNull(storeInfo);
        CoverageInfo ci = catalog.getCoverageByName(COVERAGE_NAME);
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());

        // Test on the GridCoverageStats
        FilterFunction_gridCoverageStats funcStat = new FilterFunction_gridCoverageStats();

        GridCoverageReader reader = ci.getGridCoverageReader(null, null);
        GridCoverage2D gridCoverage = (GridCoverage2D) reader.read(null);
        double min = (Double) funcStat.evaluate(gridCoverage, "minimum");
        double max = (Double) funcStat.evaluate(gridCoverage, "maximum");
        assertEquals(min, 13.1369, TOLERANCE);
        assertEquals(max, 20.665, TOLERANCE);
        ImageIOUtilities.disposeImage(gridCoverage.getRenderedImage());
    }

    @Test
    public void testGridBandStats() throws Exception {
        // check the coverage is actually there
        Catalog catalog = getCatalog();
        CoverageStoreInfo storeInfo = catalog.getCoverageStoreByName(COVERAGE_NAME);
        assertNotNull(storeInfo);
        CoverageInfo ci = catalog.getCoverageByName(COVERAGE_NAME);
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());

        // Test on the GridCoverageStats
        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Function minStat = ff.function("bandStats", ff.literal(0), ff.literal("minimum"));
        Function maxStat = ff.function("bandStats", ff.literal(0), ff.literal("maximum"));

        GridCoverageReader reader = ci.getGridCoverageReader(null, null);
        GridCoverage2D gridCoverage = (GridCoverage2D) reader.read(null);
        double min = (Double) minStat.evaluate(gridCoverage);
        double max = (Double) maxStat.evaluate(gridCoverage);
        assertEquals(min, 0, TOLERANCE);
        assertEquals(max, 0.5, TOLERANCE);
        ImageIOUtilities.disposeImage(gridCoverage.getRenderedImage());
    }

    @Test
    public void testSvgColorMapFilterFunctionRGB() throws Exception {
        final FilterFunction_svgColorMap func = new FilterFunction_svgColorMap();
        final ColorMap colorMap =
                (ColorMap)
                        func.evaluate(
                                "rgb(0,0,255);rgb(0,255,0);rgb(255,0,0)",
                                10,
                                100,
                                null,
                                null,
                                false,
                                MAX_PALETTE_COLORS);
        final ColorMapEntry[] entries = colorMap.getColorMapEntries();

        check(entries);
    }

    @Test
    public void testSvgColorMapFilterFunctionHEX() throws Exception {
        final FilterFunction_svgColorMap func = new FilterFunction_svgColorMap();
        final ColorMap colorMap =
                (ColorMap)
                        func.evaluate(
                                "#0000FF;#00FF00;#FF0000",
                                10,
                                100,
                                null,
                                null,
                                false,
                                MAX_PALETTE_COLORS);
        final ColorMapEntry[] entries = colorMap.getColorMapEntries();

        check(entries);
    }

    private void check(ColorMapEntry[] entries) {
        assertEquals(5, entries.length);

        assertColorMapEntry(entries[0], "#000000", 0.0, 10d);
        assertColorMapEntry(entries[1], "#0000FF", 1.0, 10d);
        assertColorMapEntry(entries[2], "#00FF00", 1.0, 55d);
        assertColorMapEntry(entries[3], "#FF0000", 1.0, 100d);
        assertColorMapEntry(entries[4], "#000000", 0.0, 100d);
    }

    @Test
    public void testSvgColorMapFilterFunctionRGBWithExpression() throws Exception {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        checkFunction(
                ff.function(
                        "colormap",
                        ff.literal("rgb(0,0,255);rgb(0,255,0);rgb(255,0,0)"),
                        ff.literal(10),
                        ff.literal(100)));
        checkFunction(
                ff.function(
                        "colormap",
                        ff.literal("rgb(0,0,255);rgb(0,255,0);rgb(255,0,0)"),
                        ff.literal(10),
                        ff.literal(100),
                        ff.literal(null),
                        ff.literal(null),
                        ff.literal("false"),
                        ff.literal(MAX_PALETTE_COLORS)));
    }

    private void checkFunction(Function colorMapFunction) {
        ColorMap colorMap = (ColorMap) colorMapFunction.evaluate(null);
        final ColorMapEntry[] entries = colorMap.getColorMapEntries();

        check(entries);
    }

    @Test
    public void testBeforeAfterColor() throws Exception {
        final FilterFunction_svgColorMap func = new FilterFunction_svgColorMap();
        final ColorMap colorMap =
                (ColorMap)
                        func.evaluate(
                                "#0000FF;#00FF00;#FF0000",
                                10,
                                100,
                                "#FFFFFF",
                                "#000000",
                                false,
                                MAX_PALETTE_COLORS);
        final ColorMapEntry[] entries = colorMap.getColorMapEntries();

        assertColorMapEntry(entries[0], "#FFFFFF", 1.0, 9.99);
        assertColorMapEntry(entries[1], "#0000FF", 1.0, 10d);
        assertColorMapEntry(entries[2], "#00FF00", 1.0, 55d);
        assertColorMapEntry(entries[3], "#FF0000", 1.0, 100d);
        assertColorMapEntry(entries[4], "#000000", 1.0, 100d);
    }

    void assertColorMapEntry(
            ColorMapEntry cme, String expectedColor, Double expectedOpacity, Double expectedValue) {
        if (expectedColor != null) {
            assertEquals(expectedColor, cme.getColor().evaluate(null, String.class));
        }
        if (expectedOpacity != null) {
            if (cme.getOpacity() == null || cme.getOpacity().evaluate(null) == null) {
                assertEquals(expectedOpacity, 1d, TOLERANCE);
            } else {
                assertEquals(
                        expectedOpacity, cme.getOpacity().evaluate(null, Double.class), TOLERANCE);
            }
        }
        if (expectedValue != null) {
            assertEquals(expectedValue, cme.getQuantity().evaluate(null, Double.class), TOLERANCE);
        }
    }

    @Test
    public void testLogarithmic() throws Exception {
        final FilterFunction_svgColorMap func = new FilterFunction_svgColorMap();
        final ColorMap colorMap =
                (ColorMap)
                        func.evaluate(
                                "#0000FF;#00FF00;#FF0000",
                                10,
                                100,
                                null,
                                null,
                                true,
                                MAX_PALETTE_COLORS);
        final ColorMapEntry[] entries = colorMap.getColorMapEntries();

        assertEquals(FilterFunction_svgColorMap.LOG_SAMPLING_DEFAULT + 2, entries.length);

        // first and last are transparent
        assertEquals(0, entries[0].getOpacity().evaluate(null, Double.class), TOLERANCE);
        assertEquals(
                0,
                entries[FilterFunction_svgColorMap.LOG_SAMPLING_DEFAULT + 1]
                        .getOpacity()
                        .evaluate(null, Double.class),
                TOLERANCE);

        // check the logaritmic progression
        double logMin = Math.log(10);
        double logMax = Math.log(100);
        double step = (logMax - logMin) / FilterFunction_svgColorMap.LOG_SAMPLING_DEFAULT;
        for (int i = 0; i < FilterFunction_svgColorMap.LOG_SAMPLING_DEFAULT - 1; i++) {
            final double v = logMin + step * i;
            double expected = Math.exp(v);
            assertEquals(
                    "Failed at " + i,
                    expected,
                    entries[i + 1].getQuantity().evaluate(null, Double.class),
                    TOLERANCE);
        }
        assertEquals(
                100,
                entries[FilterFunction_svgColorMap.LOG_SAMPLING_DEFAULT]
                        .getQuantity()
                        .evaluate(null, Double.class),
                TOLERANCE);
    }

    @Test
    public void testOneColor() throws Exception {
        final FilterFunction_svgColorMap func = new FilterFunction_svgColorMap();
        final ColorMap colorMap =
                (ColorMap) func.evaluate("#0000FF;#00FF00;#FF0000", 10, 100, null, null, false, 1);
        final ColorMapEntry[] entries = colorMap.getColorMapEntries();

        assertEquals(3, entries.length);
        assertColorMapEntry(entries[0], "#000000", 0.0, 10d);
        assertColorMapEntry(entries[1], "#FF0000", 1.0, 100d);
        assertColorMapEntry(entries[2], "#000000", 0.0, Double.POSITIVE_INFINITY);
    }

    @Test
    public void testTwoColors() throws Exception {
        final FilterFunction_svgColorMap func = new FilterFunction_svgColorMap();
        final ColorMap colorMap =
                (ColorMap) func.evaluate("#0000FF;#00FF00;#FF0000", 10, 100, null, null, false, 2);
        // logColorMap(colorMap);
        final ColorMapEntry[] entries = colorMap.getColorMapEntries();

        assertEquals(4, entries.length);
        assertColorMapEntry(entries[0], "#000000", 0.0, 10d);
        assertColorMapEntry(entries[1], "#0000FF", 1.0, 55d);
        assertColorMapEntry(entries[2], "#FF0000", 1.0, 100d);
        assertColorMapEntry(entries[3], "#000000", 0.0, Double.POSITIVE_INFINITY);
    }

    void logColorMap(ColorMap colorMap) {
        SLDTransformer tx = new SLDTransformer();
        tx.setIndentation(2);
        try {
            tx.transform(colorMap, System.out);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testThreeColors() throws Exception {
        final FilterFunction_svgColorMap func = new FilterFunction_svgColorMap();
        final ColorMap colorMap =
                (ColorMap) func.evaluate("#0000FF;#00FF00;#FF0000", 10, 100, null, null, false, 3);
        // logColorMap(colorMap);
        final ColorMapEntry[] entries = colorMap.getColorMapEntries();

        assertEquals(5, entries.length);
        assertColorMapEntry(entries[0], "#000000", 0.0, 10d);
        assertColorMapEntry(entries[1], "#0000FF", 1.0, 40d);
        assertColorMapEntry(entries[2], "#00A956", 1.0, 70d);
        assertColorMapEntry(entries[3], "#FF0000", 1.0, 100d);
        assertColorMapEntry(entries[4], "#000000", 0.0, Double.POSITIVE_INFINITY);
    }
}
