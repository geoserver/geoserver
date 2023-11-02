/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import it.geosolutions.imageio.pam.PAMDataset;
import it.geosolutions.imageio.pam.PAMParser;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import org.geoserver.test.GeoServerBaseTestSupport;
import org.geotools.api.style.ChannelSelection;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.SelectedChannelType;
import org.geotools.api.style.Style;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.geotools.xml.styling.SLDTransformer;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class RasterAttributeTableTest {

    static final Logger LOGGER = Logging.getLogger(RasterAttributeTableTest.class);

    private static final double EPS = 1e-6;
    PAMParser pamParser = new PAMParser();

    PAMDataset getDataset(String name) throws IOException {
        File file = new File("./src/test/resources/org/geoserver/rat/" + name);
        return pamParser.parsePAM(file);
    }

    private ColorMap getRangeColors(int bandIdx, String classification) throws IOException {
        PAMDataset dataset = getDataset("rangeColors.xml");
        assertEquals(1, dataset.getPAMRasterBand().size());
        RasterAttributeTable rat = new CoverageRATs(dataset).getRasterAttributeTable(bandIdx);
        // case with min and max values
        assertThat(rat, CoreMatchers.instanceOf(RasterAttributeTable.Categorize.class));
        // both classifications and generic fields are set
        assertThat(rat.getClassifications(), contains("Class", "Class2", "Class3"));

        ColorMap cm = getColorMap(rat, classification, bandIdx);
        return cm;
    }

    private ColorMap getRangeNoColor(int bandIdx, String classification) throws IOException {
        PAMDataset dataset = getDataset("rangeNoColor.xml");
        assertEquals(1, dataset.getPAMRasterBand().size());
        RasterAttributeTable rat = new CoverageRATs(dataset).getRasterAttributeTable(bandIdx);
        // case with min and max values
        assertThat(rat, CoreMatchers.instanceOf(RasterAttributeTable.Categorize.class));
        // both classifications and generic fields are set
        assertThat(rat.getClassifications(), contains("test"));

        ColorMap cm = getColorMap(rat, classification, bandIdx);
        return cm;
    }

    /** This dataset has two bands, single values, three classifications and colors */
    private ColorMap getValueColors(int bandIdx, String classification) throws IOException {
        PAMDataset dataset = getDataset("valueColors.xml");
        assertEquals(2, dataset.getPAMRasterBand().size());
        RasterAttributeTable rat = new CoverageRATs(dataset).getRasterAttributeTable(bandIdx);
        // case with single values
        assertThat(rat, CoreMatchers.instanceOf(RasterAttributeTable.Recode.class));
        // both classifications and generic fields are set
        assertThat(rat.getClassifications(), contains("Class", "Class2", "Class3"));

        ColorMap cm = getColorMap(rat, classification, bandIdx);
        return cm;
    }

    /** This dataset has three bands, single values, many generic fields in the third band */
    private ColorMap getValueNoColors(String classification) throws IOException {
        PAMDataset dataset = getDataset("valueNoColors.xml");
        assertEquals(3, dataset.getPAMRasterBand().size());
        RasterAttributeTable rat = new CoverageRATs(dataset).getRasterAttributeTable(2);
        // case with single values
        assertThat(rat, CoreMatchers.instanceOf(RasterAttributeTable.Recode.class));
        // many generic fields, a lot, only check a few
        assertThat(
                rat.getClassifications(),
                hasItems(
                        "data_assessment",
                        "feature_least_depth",
                        "significant_features",
                        "feature_size",
                        "bathy_coverage"));

        ColorMap cm = getColorMap(rat, classification, 2);
        return cm;
    }

    public static void assertColorMapEntry(
            ColorMapEntry first, String label, double quantity, String hexColor, int opacity) {
        assertEquals(label, first.getLabel());
        assertEquals(quantity, first.getQuantity().evaluate(null, Double.class), EPS);
        assertEquals(opacity, first.getOpacity().evaluate(null, Double.class), EPS);
        assertColor(first, hexColor);
    }

    private static void assertColor(ColorMapEntry entry, String hexColor) {
        assertEquals(color(hexColor), entry.getColor().evaluate(null, Color.class));
    }

    private static Color color(String hexColor) {
        return Converters.convert(hexColor, Color.class);
    }

    private static ColorMap getColorMap(RasterAttributeTable rat, String fieldName, int bandIdx) {
        Style style = rat.classify(fieldName);
        assertEquals(fieldName, style.getName());
        List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
        assertEquals(1, featureTypeStyles.size());
        List<Rule> rules = featureTypeStyles.get(0).rules();
        assertEquals(1, rules.size());
        Rule rule = rules.get(0);
        assertEquals(fieldName, rule.getName());
        assertEquals(1, rule.symbolizers().size());
        assertThat(rule.symbolizers().get(0), CoreMatchers.instanceOf(RasterSymbolizer.class));
        RasterSymbolizer rs = (RasterSymbolizer) rule.symbolizers().get(0);
        ChannelSelection cs = rs.getChannelSelection();
        if (bandIdx == 0) {
            assertNull(cs);
        } else {
            assertNotNull(bandIdx);
            SelectedChannelType grayChannel = cs.getGrayChannel();
            assertNotNull(grayChannel);
            assertEquals(
                    bandIdx + 1,
                    grayChannel.getChannelName().evaluate(null, Integer.class).intValue());
        }
        ColorMap cm = rs.getColorMap();
        return cm;
    }

    @Test
    public void testRangeColorsClass() throws Exception {
        ColorMap cm = getRangeColors(0, "Class");

        assertEquals(ColorMap.TYPE_INTERVALS, cm.getType());
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(4, entries.length);
        assertColorMapEntry(entries[0], "zero", -1e+25, "#000a64", 1);
        assertColorMapEntry(entries[1], "one", 3e+12, "#641400", 1);
        assertColorMapEntry(entries[2], "two", 1e+20, "#c81e32", 1);
        assertColorMapEntry(entries[3], null, 5e+25, "#ffffff", 0);
    }

    @Test
    public void testRangeColorsClass2() throws Exception {
        ColorMap cm = getRangeColors(0, "Class2");

        assertEquals(ColorMap.TYPE_INTERVALS, cm.getType());
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(4, entries.length);
        assertColorMapEntry(entries[0], "zero2", -1e+25, "#000a64", 1);
        assertColorMapEntry(entries[1], "one2", 3e+12, "#641400", 1);
        assertColorMapEntry(entries[2], "zero2", 1e+20, "#c81e32", 1);
        assertColorMapEntry(entries[3], null, 5e+25, "#ffffff", 0);
    }

    @Test
    public void testRangeColorsClass3() throws Exception {
        ColorMap cm = getRangeColors(0, "Class3"); // generic field, not a name field

        assertEquals(ColorMap.TYPE_INTERVALS, cm.getType());
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(4, entries.length);
        assertColorMapEntry(entries[0], "zero3", -1e+25, "#000a64", 1);
        assertColorMapEntry(entries[1], "one3", 3e+12, "#641400", 1);
        assertColorMapEntry(entries[2], "two3", 1e+20, "#c81e32", 1);
        assertColorMapEntry(entries[3], null, 5e+25, "#ffffff", 0);
    }

    @Test
    public void testRangeNoColor() throws Exception {
        // this one has ranges, with empty bits in the middle between one class and the next
        ColorMap cm = getRangeNoColor(0, "test");
        printStyle(cm);

        // to handle the holes, classes that are transparent and have no label are added
        // the fact the classe are color names makes the test expectations look odd
        // the "random" colors are stable, always using the same seed
        assertEquals(ColorMap.TYPE_INTERVALS, cm.getType());
        assertRangesNoColor(cm);
        // and so on, not testing them all
    }

    public static void assertRangesNoColor(ColorMap cm) {
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(16, entries.length);
        assertColorMapEntry(entries[0], "green", 1, "#CDAD4E", 1);
        assertColorMapEntry(entries[1], null, 1.2, "#FFFFFF", 0);
        assertColorMapEntry(entries[2], "white", 1.4, "#FD02BE", 1);
        assertColorMapEntry(entries[3], null, 1.6, "#FFFFFF", 0);
        assertColorMapEntry(entries[4], "gold", 2.4, "#2EE3E3", 1);
        assertColorMapEntry(entries[5], null, 2.6, "#FFFFFF", 0);
    }

    @Test
    public void testValueColors0Class() throws Exception {
        // two bands, three classification each
        ColorMap cm = getValueColors(0, "Class");

        assertEquals(ColorMap.TYPE_VALUES, cm.getType());
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(3, entries.length);
        assertColorMapEntry(entries[0], "zero", 0, "#000a64", 1);
        assertColorMapEntry(entries[1], "one", 2, "#641400", 1);
        assertColorMapEntry(entries[2], "two", 4, "#c81e32", 1);
    }

    @Test
    public void testValueColors1Class2() throws Exception {
        // two bands, three classification each
        ColorMap cm = getValueColors(1, "Class2");

        assertEquals(ColorMap.TYPE_VALUES, cm.getType());
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(3, entries.length);
        assertColorMapEntry(entries[0], "one2", 1, "#64140a", 1);
        assertColorMapEntry(entries[1], "three2", 3, "#c80a14", 1);
        assertColorMapEntry(entries[2], "five2", 5, "#3228fa", 1);
    }

    @Test
    public void testValueNoColors() throws Exception {
        // two bands, three classification each
        ColorMap cm = getValueNoColors("data_assessment");
        printStyle(cm);

        assertEquals(ColorMap.TYPE_VALUES, cm.getType());
        ColorMapEntry[] entries = cm.getColorMapEntries();
        assertEquals(2, entries.length);
        assertColorMapEntry(entries[0], "3", 0, "#4ECDCD", 1);
        assertColorMapEntry(entries[1], "1", 1544412, "#EA1F1F", 1);
    }

    private static void printStyle(ColorMap cm) throws TransformerException {
        if (!GeoServerBaseTestSupport.isQuietTests() && LOGGER.isLoggable(Level.INFO)) {
            SLDTransformer tx = new SLDTransformer();
            tx.setIndentation(2);
            LOGGER.info(tx.transform(cm));
        }
    }

    @Test
    public void testNoRasterAttributeTable() throws Exception {
        PAMDataset dataset = getDataset("norat.xml");
        assertEquals(1, dataset.getPAMRasterBand().size());
        RasterAttributeTable rat = new CoverageRATs(dataset).getRasterAttributeTable(0);
        assertNull(rat);
    }
}
