/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.renderer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gsr.controller.ControllerTest;
import org.geoserver.gsr.model.font.Font;
import org.geoserver.gsr.model.font.FontStyleEnum;
import org.geoserver.gsr.model.font.FontWeightEnum;
import org.geoserver.gsr.model.label.Label;
import org.geoserver.gsr.model.renderer.ClassBreakInfo;
import org.geoserver.gsr.model.renderer.ClassBreaksRenderer;
import org.geoserver.gsr.model.renderer.Renderer;
import org.geoserver.gsr.model.renderer.UniqueValueInfo;
import org.geoserver.gsr.model.renderer.UniqueValueRenderer;
import org.geoserver.gsr.model.symbol.HorizontalAlignmentEnum;
import org.geoserver.gsr.model.symbol.SimpleFillSymbol;
import org.geoserver.gsr.model.symbol.SimpleFillSymbolEnum;
import org.geoserver.gsr.model.symbol.SimpleLineSymbol;
import org.geoserver.gsr.model.symbol.SimpleLineSymbolEnum;
import org.geoserver.gsr.model.symbol.TextSymbol;
import org.geoserver.gsr.model.symbol.VerticalAlignmentEnum;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.StyleFactory;
import org.geotools.xml.styling.SLDParser;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.opengis.style.Style;

public class RendererEncoderTest extends ControllerTest {

    @Test
    public void testPolygonRendererJsonSchema() throws Exception {
        StyleInfo polygonInfo = getGeoServer().getCatalog().getStyleByName("Lakes");
        assertNotNull(polygonInfo);
        Style polygon = polygonInfo.getStyle();
        assertNotNull(polygon);
        Renderer polygonRenderer =
                StyleEncoder.styleToRenderer((org.geotools.styling.Style) polygon);
        assertNotNull(polygonRenderer);
        JSONBuilder json = new JSONStringer();
        StyleEncoder.encodeRenderer(json, polygonRenderer);
    }

    @Test
    public void testLineRenderer() throws Exception {
        StyleInfo lineInfo = getGeoServer().getCatalog().getStyleByName("Streams");
        assertNotNull(lineInfo);
        Style line = lineInfo.getStyle();
        assertNotNull(line);
        Renderer lineRenderer = StyleEncoder.styleToRenderer((org.geotools.styling.Style) line);
        assertNotNull(lineRenderer);
        JSONBuilder json = new JSONStringer();
        StyleEncoder.encodeRenderer(json, lineRenderer);
    }

    @Test
    public void testPointRenderer() throws Exception {
        StyleInfo pointInfo = getGeoServer().getCatalog().getStyleByName("Buildings");
        assertNotNull(pointInfo);
        Style point = pointInfo.getStyle();
        assertNotNull(point);
        Renderer pointRenderer = StyleEncoder.styleToRenderer((org.geotools.styling.Style) point);
        assertNotNull(point);
        JSONBuilder json = new JSONStringer();
        StyleEncoder.encodeRenderer(json, pointRenderer);
    }

    private Renderer parseAndConvertToRenderer(String sldPath) throws Exception {
        StyleFactory factory = CommonFactoryFinder.getStyleFactory();
        SLDParser parser = new SLDParser(factory, getClass().getResource(sldPath));
        org.geotools.styling.Style sld = parser.readXML()[0];
        return StyleEncoder.styleToRenderer(sld);
    }

    private List<Label> parseAndConvertToLabelInfo(String sldPath) throws Exception {
        StyleFactory factory = CommonFactoryFinder.getStyleFactory();
        SLDParser parser = new SLDParser(factory, getClass().getResource(sldPath));
        org.geotools.styling.Style sld = parser.readXML()[0];
        return StyleEncoder.styleToLabel(sld);
    }

    @Test
    public void testIconRenderer() throws Exception {
        StyleFactory factory = CommonFactoryFinder.getStyleFactory();
        SLDParser parser = new SLDParser(factory, getClass().getResource("mark.sld"));
        org.geotools.styling.Style sld = parser.readXML()[0];
        JSONBuilder json = new JSONStringer();
        Renderer renderer = StyleEncoder.styleToRenderer(sld);
        assertNotNull(renderer);
        StyleEncoder.encodeRenderer(json, renderer);
        JSONObject object = JSONObject.fromObject(json.toString());
        JSONObject symbol = object.getJSONObject("symbol");
        String url = symbol.getString("url");
        String contentType = symbol.getString("contentType");
        int width = symbol.getInt("width");
        int height = symbol.getInt("height");
        assertTrue(url.endsWith("example.jpg"));
        assertEquals("image/jpeg", contentType);
        assertEquals(64, width);
        assertEquals(64, height);
    }

    @Test
    public void testClassBreaks() throws Exception {
        Renderer renderer = parseAndConvertToRenderer("earthquakes.sld");
        assertTrue(renderer.toString(), renderer instanceof ClassBreaksRenderer);
        renderer = parseAndConvertToRenderer("hnd_bridges_graduated.sld");
        assertTrue(renderer.toString(), renderer instanceof ClassBreaksRenderer);
    }

    @Test
    public void testUniqueValues() throws Exception {
        Renderer renderer = parseAndConvertToRenderer("hnd_cemeteries_categorized.sld");
        assertTrue(renderer.toString(), renderer instanceof UniqueValueRenderer);
    }

    @Test
    public void testPopShade() throws Exception {
        // this one has lower, between and greater
        Renderer renderer = parseAndConvertToRenderer("popshade.sld");
        assertTrue(renderer.toString(), renderer instanceof ClassBreaksRenderer);

        ClassBreaksRenderer cbr = (ClassBreaksRenderer) renderer;
        assertEquals("PERSONS", cbr.getField());
        assertEquals(-Double.MAX_VALUE, cbr.getMinValue(), 0d);
        List<ClassBreakInfo> breaks = cbr.getClassBreakInfos();
        assertEquals(3, breaks.size());
        ClassBreakInfo break1 = breaks.get(0);
        assertEquals(-Double.MAX_VALUE, break1.getClassMinValue(), 0d);
        assertEquals(2000000, break1.getClassMaxValue(), 0d);
        SimpleFillSymbol symbol1 = (SimpleFillSymbol) break1.getSymbol();
        assertEquals(SimpleFillSymbolEnum.SOLID, symbol1.getStyle());
        ClassBreakInfo break2 = breaks.get(1);
        assertEquals(2000000, break2.getClassMinValue(), 0d);
        assertEquals(4000000, break2.getClassMaxValue(), 0d);
        SimpleFillSymbol symbol2 = (SimpleFillSymbol) break2.getSymbol();
        assertEquals(SimpleFillSymbolEnum.SOLID, symbol2.getStyle());
        ClassBreakInfo break3 = breaks.get(2);
        assertEquals(4000000, break3.getClassMinValue(), 0d);
        assertEquals(Double.MAX_VALUE, break3.getClassMaxValue(), 0d);
        SimpleFillSymbol symbol3 = (SimpleFillSymbol) break3.getSymbol();
        assertEquals(SimpleFillSymbolEnum.SOLID, symbol3.getStyle());
    }

    @Test
    public void testPopHatch() throws Exception {
        // this one has lower, between and greater
        Renderer renderer = parseAndConvertToRenderer("pophatch.sld");
        assertTrue(renderer.toString(), renderer instanceof ClassBreaksRenderer);

        ClassBreaksRenderer cbr = (ClassBreaksRenderer) renderer;
        assertEquals("PERSONS", cbr.getField());
        assertEquals(-Double.MAX_VALUE, cbr.getMinValue(), 0d);
        List<ClassBreakInfo> breaks = cbr.getClassBreakInfos();
        assertEquals(3, breaks.size());
        ClassBreakInfo break1 = breaks.get(0);
        assertEquals(-Double.MAX_VALUE, break1.getClassMinValue(), 0d);
        assertEquals(2000000, break1.getClassMaxValue(), 0d);
        SimpleFillSymbol symbol1 = (SimpleFillSymbol) break1.getSymbol();
        assertEquals(SimpleFillSymbolEnum.FORWARD_DIAGONAL, symbol1.getStyle());
        ClassBreakInfo break2 = breaks.get(1);
        assertEquals(2000000, break2.getClassMinValue(), 0d);
        assertEquals(4000000, break2.getClassMaxValue(), 0d);
        SimpleFillSymbol symbol2 = (SimpleFillSymbol) break2.getSymbol();
        assertEquals(SimpleFillSymbolEnum.CROSS, symbol2.getStyle());
        ClassBreakInfo break3 = breaks.get(2);
        assertEquals(4000000, break3.getClassMinValue(), 0d);
        assertEquals(Double.MAX_VALUE, break3.getClassMaxValue(), 0d);
        SimpleFillSymbol symbol3 = (SimpleFillSymbol) break3.getSymbol();
        assertEquals(SimpleFillSymbolEnum.DIAGONAL_CROSS, symbol3.getStyle());
    }

    @Test
    public void testPopShadeLabels() throws Exception {
        // this one has lower, between and greater
        List<Label> labels = parseAndConvertToLabelInfo("popshade.sld");
        assertNotNull(labels);
        assertEquals(2, labels.size());

        // first label (lower scales)
        Label label0 = labels.get(0);
        assertEquals("[STATE_ABBR]", label0.getLabelExpression());
        assertEquals(0, label0.getMinScale());
        assertEquals(15000000, label0.getMaxScale());
        assertNull(label0.getWhere());

        TextSymbol ts0 = label0.getSymbol();
        assertArrayEquals(new int[] {0, 0, 0, 255}, ts0.getColor());
        assertEquals(HorizontalAlignmentEnum.CENTER, ts0.getHorizontalAlignment());
        assertEquals(VerticalAlignmentEnum.MIDDLE, ts0.getVerticalAlignment());
        Font font0 = ts0.getFont();
        assertEquals("Times New Roman", font0.getFamily());
        assertEquals(14, font0.getSize());

        // second label (higher scales)
        Label label1 = labels.get(1);
        assertEquals("[STATE_NAME]", label1.getLabelExpression());
        assertEquals(15000000, label1.getMinScale());
        assertEquals(0, label1.getMaxScale());
        assertNull(label1.getWhere());

        TextSymbol ts1 = label1.getSymbol();
        assertArrayEquals(new int[] {68, 68, 68, 255}, ts1.getColor());
        assertEquals(HorizontalAlignmentEnum.CENTER, ts1.getHorizontalAlignment());
        assertEquals(VerticalAlignmentEnum.MIDDLE, ts1.getVerticalAlignment());
        assertArrayEquals(new int[] {255, 255, 255, 255}, ts1.getHaloColor());
        assertEquals(Integer.valueOf(2), ts1.getHaloSize());
        Font font1 = ts1.getFont();
        assertEquals("Arial", font1.getFamily());
        assertEquals(18, font1.getSize());
        assertEquals(FontStyleEnum.ITALIC, font1.getStyle());
        assertEquals(FontWeightEnum.BOLD, font1.getWeight());
    }

    @Test
    public void testDashArray() throws Exception {
        Renderer renderer = parseAndConvertToRenderer("dasharray.sld");
        UniqueValueRenderer uvr = (UniqueValueRenderer) renderer;

        // first is solid
        SimpleLineSymbol line0 = (SimpleLineSymbol) uvr.getUniqueValueInfos().get(0).getSymbol();
        assertEquals(SimpleLineSymbolEnum.SOLID, line0.getStyle());

        // second recognized as dash
        SimpleLineSymbol line1 = (SimpleLineSymbol) uvr.getUniqueValueInfos().get(1).getSymbol();
        assertEquals(SimpleLineSymbolEnum.DASH, line1.getStyle());

        // third recognized as dot
        SimpleLineSymbol line2 = (SimpleLineSymbol) uvr.getUniqueValueInfos().get(2).getSymbol();
        assertEquals(SimpleLineSymbolEnum.DOT, line2.getStyle());

        // fourth recognized as dash dot
        SimpleLineSymbol line3 = (SimpleLineSymbol) uvr.getUniqueValueInfos().get(3).getSymbol();
        assertEquals(SimpleLineSymbolEnum.DASH_DOT, line3.getStyle());

        // fourth recognized as dash dot dot
        SimpleLineSymbol line4 = (SimpleLineSymbol) uvr.getUniqueValueInfos().get(4).getSymbol();
        assertEquals(SimpleLineSymbolEnum.DASH_DOT_DOT, line4.getStyle());

        // fifth not recognized, assigned as a dash as default
        SimpleLineSymbol line5 = (SimpleLineSymbol) uvr.getUniqueValueInfos().get(5).getSymbol();
        assertEquals(SimpleLineSymbolEnum.DASH, line5.getStyle());
    }

    @Test
    public void testRecodeFill() throws Exception {
        Renderer renderer = parseAndConvertToRenderer("states_recode_fill.sld");
        assertThat(renderer, CoreMatchers.instanceOf(UniqueValueRenderer.class));
        UniqueValueRenderer uvr = (UniqueValueRenderer) renderer;

        assertEquals("SUB_REGION", uvr.getField1());
        List<UniqueValueInfo> values = uvr.getUniqueValueInfos();
        assertEquals(9, values.size());

        // first value
        UniqueValueInfo v0 = values.get(0);
        assertEquals("N Eng", v0.getValue());
        SimpleFillSymbol fill0 = (SimpleFillSymbol) v0.getSymbol();
        assertArrayEquals(new int[] {100, 149, 237, 255}, fill0.getColor());
        assertArrayEquals(new int[] {170, 170, 170, 255}, fill0.getOutline().getColor());

        // mid value
        UniqueValueInfo v3 = values.get(3);
        assertEquals("E N Cen", v3.getValue());
        SimpleFillSymbol fill3 = (SimpleFillSymbol) v3.getSymbol();
        assertArrayEquals(new int[] {154, 205, 50, 255}, fill3.getColor());
        assertArrayEquals(new int[] {170, 170, 170, 255}, fill3.getOutline().getColor());

        // last value
        UniqueValueInfo v8 = values.get(8);
        assertEquals("Pacific", v8.getValue());
        SimpleFillSymbol fill8 = (SimpleFillSymbol) v8.getSymbol();
        assertArrayEquals(new int[] {135, 206, 235, 255}, fill8.getColor());
        assertArrayEquals(new int[] {170, 170, 170, 255}, fill8.getOutline().getColor());
    }

    @Test
    public void testRecodeFillStroke() throws Exception {
        Renderer renderer = parseAndConvertToRenderer("states_recode_fill_stroke.sld");
        assertThat(renderer, CoreMatchers.instanceOf(UniqueValueRenderer.class));
        UniqueValueRenderer uvr = (UniqueValueRenderer) renderer;

        assertEquals("SUB_REGION", uvr.getField1());
        List<UniqueValueInfo> values = uvr.getUniqueValueInfos();
        assertEquals(9, values.size());

        // first value
        UniqueValueInfo v0 = values.get(0);
        assertEquals("N Eng", v0.getValue());
        SimpleFillSymbol fill0 = (SimpleFillSymbol) v0.getSymbol();
        assertArrayEquals(new int[] {100, 149, 237, 255}, fill0.getColor());
        assertArrayEquals(new int[] {0, 0, 0, 255}, fill0.getOutline().getColor());
        assertEquals(0, fill0.getOutline().getWidth(), 0d);

        // mid value
        UniqueValueInfo v3 = values.get(3);
        assertEquals("E N Cen", v3.getValue());
        SimpleFillSymbol fill3 = (SimpleFillSymbol) v3.getSymbol();
        assertArrayEquals(new int[] {154, 205, 50, 255}, fill3.getColor());
        assertArrayEquals(new int[] {0, 0, 0, 255}, fill3.getOutline().getColor());
        assertEquals(3, fill3.getOutline().getWidth(), 0d);

        // last value
        UniqueValueInfo v8 = values.get(8);
        assertEquals("Pacific", v8.getValue());
        SimpleFillSymbol fill8 = (SimpleFillSymbol) v8.getSymbol();
        System.out.println(Arrays.toString(fill8.getColor()));
        assertArrayEquals(new int[] {135, 206, 235, 255}, fill8.getColor());
        assertArrayEquals(new int[] {0, 0, 0, 255}, fill8.getOutline().getColor());
        assertEquals(8, fill8.getOutline().getWidth(), 0d);
    }

    @Test
    public void testRecodeFillStrokeMisaligned() throws Exception {
        // two recodes but using different key values, cannot be translated
        Renderer renderer = parseAndConvertToRenderer("states_recode_misaligned.sld");
        assertNull(renderer);
    }

    @Test
    public void testCategorizeFill() throws Exception {
        Renderer renderer = parseAndConvertToRenderer("states_categorize_fill.sld");
        assertThat(renderer, CoreMatchers.instanceOf(ClassBreaksRenderer.class));
        ClassBreaksRenderer cbr = (ClassBreaksRenderer) renderer;

        assertEquals("LAND_KM", cbr.getField());
        assertEquals(-Double.MAX_VALUE, cbr.getMinValue(), 0d);
        List<ClassBreakInfo> breaks = cbr.getClassBreakInfos();
        assertEquals(3, breaks.size());

        // first
        ClassBreakInfo cb0 = breaks.get(0);
        assertEquals(100000, cb0.getClassMaxValue(), 0d);
        SimpleFillSymbol fill0 = (SimpleFillSymbol) cb0.getSymbol();
        assertArrayEquals(new int[] {135, 206, 235, 255}, fill0.getColor());
        assertArrayEquals(new int[] {0, 0, 0, 255}, fill0.getOutline().getColor());
        assertEquals(1, fill0.getOutline().getWidth(), 0d);

        // mid
        ClassBreakInfo cb1 = breaks.get(1);
        assertEquals(200000, cb1.getClassMaxValue(), 0d);
        SimpleFillSymbol fill1 = (SimpleFillSymbol) cb1.getSymbol();
        assertArrayEquals(new int[] {255, 250, 205, 255}, fill1.getColor());
        assertArrayEquals(new int[] {0, 0, 0, 255}, fill1.getOutline().getColor());
        assertEquals(1, fill0.getOutline().getWidth(), 0d);

        // last
        ClassBreakInfo cb2 = breaks.get(2);
        assertEquals(Double.MAX_VALUE, cb2.getClassMaxValue(), 0d);
        SimpleFillSymbol fill2 = (SimpleFillSymbol) cb2.getSymbol();
        assertArrayEquals(new int[] {240, 128, 128, 255}, fill2.getColor());
        assertArrayEquals(new int[] {0, 0, 0, 255}, fill2.getOutline().getColor());
        assertEquals(1, fill0.getOutline().getWidth(), 0d);
    }

    @Test
    public void testCategorizeFillStroke() throws Exception {
        Renderer renderer = parseAndConvertToRenderer("states_categorize_fill.sld");
        assertThat(renderer, CoreMatchers.instanceOf(ClassBreaksRenderer.class));
        ClassBreaksRenderer cbr = (ClassBreaksRenderer) renderer;

        assertEquals("LAND_KM", cbr.getField());
        assertEquals(-Double.MAX_VALUE, cbr.getMinValue(), 0d);
        List<ClassBreakInfo> breaks = cbr.getClassBreakInfos();
        assertEquals(3, breaks.size());

        // first
        ClassBreakInfo cb0 = breaks.get(0);
        assertEquals(100000, cb0.getClassMaxValue(), 0d);
        SimpleFillSymbol fill0 = (SimpleFillSymbol) cb0.getSymbol();
        assertArrayEquals(new int[] {135, 206, 235, 255}, fill0.getColor());
        assertArrayEquals(new int[] {0, 0, 0, 255}, fill0.getOutline().getColor());
        assertEquals(1, fill0.getOutline().getWidth(), 0d);

        // mid
        ClassBreakInfo cb1 = breaks.get(1);
        assertEquals(200000, cb1.getClassMaxValue(), 0d);
        SimpleFillSymbol fill1 = (SimpleFillSymbol) cb1.getSymbol();
        assertArrayEquals(new int[] {255, 250, 205, 255}, fill1.getColor());
        assertArrayEquals(new int[] {0, 0, 0, 255}, fill1.getOutline().getColor());
        assertEquals(1, fill1.getOutline().getWidth(), 0d);

        // last
        ClassBreakInfo cb2 = breaks.get(2);
        assertEquals(Double.MAX_VALUE, cb2.getClassMaxValue(), 0d);
        SimpleFillSymbol fill2 = (SimpleFillSymbol) cb2.getSymbol();
        assertArrayEquals(new int[] {240, 128, 128, 255}, fill2.getColor());
        assertArrayEquals(new int[] {0, 0, 0, 255}, fill2.getOutline().getColor());
        assertEquals(1, fill2.getOutline().getWidth(), 0d);
    }

    @Test
    public void testCategorizeFillStrokeMisaligned() throws Exception {
        // two recodes but using different key values, cannot be translated
        Renderer renderer = parseAndConvertToRenderer("states_categorize_misaligned.sld");
        assertNull(renderer);
    }
}
