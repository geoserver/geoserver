/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.Cell.ColorMapEntryLegendBuilder;
import org.geotools.styling.ColorMap;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.junit.Before;
import org.junit.Test;

public class RasterLegendBuilderTest {

    GetLegendGraphicRequest request;

    @Before
    public void setup() {
        request = new GetLegendGraphicRequest();
    }

    @Test
    public void testRuleTextRampOneElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null},
                        new double[] {10},
                        new Color[] {Color.RED},
                        ColorMap.TYPE_RAMP);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(1, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("", firstRow.getRuleManager().text);
    }

    @Test
    public void testRuleTextRampTwoElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null},
                        new double[] {10, 100},
                        new Color[] {Color.RED, Color.BLUE},
                        ColorMap.TYPE_RAMP);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("10.0 >= x", firstRow.getRuleManager().text);
        ColorMapEntryLegendBuilder lastRow = rows.get(1);
        assertEquals("100.0 <= x", lastRow.getRuleManager().text);
    }

    @Test
    public void testRuleTextRampThreeElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null, null},
                        new double[] {10, 50, 100},
                        new Color[] {Color.RED, Color.WHITE, Color.BLUE},
                        ColorMap.TYPE_RAMP);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(3, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("10.0 >= x", firstRow.getRuleManager().text);
        ColorMapEntryLegendBuilder midRow = rows.get(1);
        assertEquals("50.0 = x", midRow.getRuleManager().text);
        ColorMapEntryLegendBuilder lastRow = rows.get(2);
        assertEquals("100.0 <= x", lastRow.getRuleManager().text);
    }

    @Test
    public void testRuleTextIntervalOneElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null},
                        new double[] {10},
                        new Color[] {Color.RED},
                        ColorMap.TYPE_INTERVALS);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(1, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("x < 10.0", firstRow.getRuleManager().text);
    }

    @Test
    public void testRuleTextIntervalsTwoElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null},
                        new double[] {10, 100},
                        new Color[] {Color.RED, Color.BLUE},
                        ColorMap.TYPE_INTERVALS);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("x < 10.0", firstRow.getRuleManager().text);
        ColorMapEntryLegendBuilder lastRow = rows.get(1);
        assertEquals("10.0 <= x < 100.0", lastRow.getRuleManager().text);
    }

    @Test
    public void testRuleTextIntervalsThreeElements() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null, null},
                        new double[] {10, 50, 100},
                        new Color[] {Color.RED, Color.WHITE, Color.BLUE},
                        ColorMap.TYPE_INTERVALS);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(3, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("x < 10.0", firstRow.getRuleManager().text);
        ColorMapEntryLegendBuilder midRow = rows.get(1);
        assertEquals("10.0 <= x < 50.0", midRow.getRuleManager().text);
        ColorMapEntryLegendBuilder lastRow = rows.get(2);
        assertEquals("50.0 <= x < 100.0", lastRow.getRuleManager().text);
    }

    @Test
    public void testInfiniteOnIntervals() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null, null},
                        new double[] {Double.NEGATIVE_INFINITY, 50, Double.POSITIVE_INFINITY},
                        new Color[] {Color.RED, Color.WHITE, Color.BLUE},
                        ColorMap.TYPE_INTERVALS);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals("x < 50.0", firstRow.getRuleManager().text);
        ColorMapEntryLegendBuilder midRow = rows.get(1);
        assertEquals("50.0 <= x", midRow.getRuleManager().text);
    }

    @Test
    public void testLegendBorderColour() {
        StyleBuilder sb = new StyleBuilder();
        ColorMap cmap =
                sb.createColorMap(
                        new String[] {null, null, null},
                        new double[] {Double.NEGATIVE_INFINITY, 50, Double.POSITIVE_INFINITY},
                        new Color[] {Color.RED, Color.WHITE, Color.BLUE},
                        ColorMap.TYPE_INTERVALS);
        Style style = sb.createStyle(sb.createRasterSymbolizer(cmap, 1));

        // Check default border colour
        Color colourToTest = LegendUtils.DEFAULT_BORDER_COLOR;

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        assertEquals(colourToTest, firstRow.getColorManager().borderColor);
        assertEquals(colourToTest, firstRow.getRuleManager().borderColor);
        ColorMapEntryLegendBuilder midRow = rows.get(1);
        assertEquals(colourToTest, midRow.getColorManager().borderColor);
        assertEquals(colourToTest, midRow.getRuleManager().borderColor);

        // Change legend border colour to red
        Map<String, Object> legendOptions = new HashMap<String, Object>();

        colourToTest = Color.red;

        legendOptions.put("BORDERCOLOR", SLD.toHTMLColor(colourToTest));

        request.setLegendOptions(legendOptions);
        helper = new RasterLayerLegendHelper(request, style, null);
        rows = new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        firstRow = rows.get(0);
        assertEquals(colourToTest, firstRow.getColorManager().borderColor);
        assertEquals(colourToTest, firstRow.getRuleManager().borderColor);
        midRow = rows.get(1);
        assertEquals(colourToTest, midRow.getColorManager().borderColor);
        assertEquals(colourToTest, midRow.getRuleManager().borderColor);

        // Change legend border colour to blue
        colourToTest = Color.blue;

        legendOptions.clear();
        legendOptions.put("borderColor", SLD.toHTMLColor(colourToTest));

        request.setLegendOptions(legendOptions);
        helper = new RasterLayerLegendHelper(request, style, null);
        rows = new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(2, rows.size());
        firstRow = rows.get(0);
        assertEquals(colourToTest, firstRow.getColorManager().borderColor);
        assertEquals(colourToTest, firstRow.getRuleManager().borderColor);
        midRow = rows.get(1);
        assertEquals(colourToTest, midRow.getColorManager().borderColor);
        assertEquals(colourToTest, midRow.getRuleManager().borderColor);
    }
}
