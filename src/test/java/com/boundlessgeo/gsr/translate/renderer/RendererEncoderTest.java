/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.translate.renderer;

import com.boundlessgeo.gsr.controller.ControllerTest;
import com.boundlessgeo.gsr.model.font.Font;
import com.boundlessgeo.gsr.model.font.FontStyleEnum;
import com.boundlessgeo.gsr.model.font.FontWeightEnum;
import com.boundlessgeo.gsr.model.label.Label;
import com.boundlessgeo.gsr.model.renderer.ClassBreakInfo;
import com.boundlessgeo.gsr.model.renderer.ClassBreaksRenderer;
import com.boundlessgeo.gsr.model.renderer.Renderer;
import com.boundlessgeo.gsr.model.renderer.UniqueValueRenderer;
import com.boundlessgeo.gsr.model.symbol.HorizontalAlignmentEnum;
import com.boundlessgeo.gsr.model.symbol.SimpleFillSymbol;
import com.boundlessgeo.gsr.model.symbol.SimpleFillSymbolEnum;
import com.boundlessgeo.gsr.model.symbol.TextSymbol;
import com.boundlessgeo.gsr.model.symbol.VerticalAlignmentEnum;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;
import org.geoserver.catalog.StyleInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.StyleFactory;
import org.geotools.xml.styling.SLDParser;
import org.junit.Test;
import org.opengis.style.Style;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

}
