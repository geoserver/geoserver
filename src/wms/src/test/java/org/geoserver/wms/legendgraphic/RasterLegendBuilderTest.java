/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import static org.junit.Assert.assertEquals;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.Cell.ColorMapEntryLegendBuilder;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyleFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLD;
import org.geotools.styling.StyleBuilder;
import org.geotools.xml.styling.SLDParser;
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
                sb.createColorMap(new String[] {null}, new double[] {10}, new Color[] {Color.RED}, ColorMap.TYPE_RAMP);
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
        ColorMap cmap = sb.createColorMap(
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
        ColorMap cmap = sb.createColorMap(
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
        ColorMap cmap = sb.createColorMap(
                new String[] {null}, new double[] {10}, new Color[] {Color.RED}, ColorMap.TYPE_INTERVALS);
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
        ColorMap cmap = sb.createColorMap(
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
        ColorMap cmap = sb.createColorMap(
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
        ColorMap cmap = sb.createColorMap(
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
        ColorMap cmap = sb.createColorMap(
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
        Map<String, Object> legendOptions = new HashMap<>();

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

    @Test
    /** Test that the legend is correctly built when env variables are used but no values are specified */
    public void testLegendWithEnvDefaultValues() throws IOException {
        Style style = readSLD("ColorMapWithEnv.sld");

        RasterLayerLegendHelper helper = new RasterLayerLegendHelper(request, style, null);
        List<ColorMapEntryLegendBuilder> rows =
                new ArrayList<>(helper.getcMapLegendCreator().getBodyRows());
        assertEquals(3, rows.size());
        ColorMapEntryLegendBuilder firstRow = rows.get(0);
        // First entry has 0.5 opacity so a black color with half transparency
        Color halfTransparentBlack = new Color(0, 0, 0, 127);
        assertEquals(halfTransparentBlack, firstRow.get(0).bkgColor);
        // Default value for quantity env is 0.0 so the text should be built accordingly
        assertEquals("x < 0.0", firstRow.get(1).text);
        // First entry has hardcoded label "Low"
        assertEquals("Low", firstRow.get(2).text);

        ColorMapEntryLegendBuilder midRow = rows.get(1);
        // Second entry has env variable for color but no value specified,
        // with default set to green. So a green with half transparency
        // due to the 0.5 opacity hardcoded in the SLD
        Color halfTransparentGreen = new Color(0, 255, 0, 127);
        assertEquals(halfTransparentGreen, midRow.get(0).bkgColor);
        // Default value for quantity env is 100.0
        assertEquals("0.0 <= x < 100.0", midRow.get(1).text);
        // Second entry has env variable for label but no value specified,
        // with default value of "Nominal"
        assertEquals("Nominal", midRow.get(2).text);

        ColorMapEntryLegendBuilder lastRow = rows.get(2);
        // Third entry has env variable for opacity but no value specified,
        // with default set to 1, so fully opaque
        assertEquals(Color.RED, lastRow.get(0).bkgColor);
        // Default value for quantity env is 1000.0
        assertEquals("100.0 <= x < 1000.0", lastRow.get(1).text);
        assertEquals("High", lastRow.get(2).text);
    }

    private Style readSLD(String sldName) throws IOException {
        StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
        SLDParser stylereader = new SLDParser(styleFactory, getClass().getResource(sldName));
        Style[] readStyles = stylereader.readXML();

        Style style = readStyles[0];
        return style;
    }
}
