/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.label;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import com.boundlessgeo.gsr.JsonSchemaTest;
import com.boundlessgeo.gsr.model.font.Font;
import com.boundlessgeo.gsr.model.font.FontDecorationEnum;
import com.boundlessgeo.gsr.model.font.FontStyleEnum;
import com.boundlessgeo.gsr.model.font.FontWeightEnum;
import com.boundlessgeo.gsr.model.symbol.HorizontalAlignmentEnum;
import com.boundlessgeo.gsr.model.symbol.TextSymbol;
import com.boundlessgeo.gsr.model.symbol.VerticalAlignmentEnum;

public class LabelSchemaTest extends JsonSchemaTest {

    @Test
    public void testPointLabelJsonSchema() throws Exception {
        int[] color = { 78, 78, 78, 255 };
        int[] backgroundColor = null;
        int[] borderLineColor = null;
        Font font = new Font("Arial", 12, FontStyleEnum.NORMAL, FontWeightEnum.BOLD,
                FontDecorationEnum.NONE);
        TextSymbol textSymbol = new TextSymbol(0, 0, 0, color, backgroundColor, borderLineColor,
                VerticalAlignmentEnum.BOTTOM, HorizontalAlignmentEnum.LEFT, false, font);
        Label pointLabel = new PointLabel(PointLabelPlacementEnum.ABOVE_RIGHT, "[NAME]", false,
                textSymbol, 0, 0);
        String json = getJson(pointLabel);
        assertTrue(validateJSON(json, "gsr/1.0/label.json"));
    }

    @Test
    public void testLineLabelJsonSchema() throws Exception {
        int[] color = { 78, 78, 78, 255 };
        int[] backgroundColor = null;
        int[] borderLineColor = null;
        Font font = new Font("Arial", 12, FontStyleEnum.NORMAL, FontWeightEnum.BOLD,
                FontDecorationEnum.NONE);
        TextSymbol textSymbol = new TextSymbol(0, 0, 0, color, backgroundColor, borderLineColor,
                VerticalAlignmentEnum.BOTTOM, HorizontalAlignmentEnum.LEFT, false, font);
        Label lineLabel = new LineLabel(LineLabelPlacementEnum.ABOVE_BEFORE, "[NAME]", false,
                textSymbol, 0, 0);
        String json = getJson(lineLabel);
        assertTrue(validateJSON(json, "gsr/1.0/label.json"));
    }

    @Test
    public void testPolygonLabelJsonSchema() throws Exception {
        int[] color = { 78, 78, 78, 255 };
        int[] backgroundColor = null;
        int[] borderLineColor = null;
        Font font = new Font("Arial", 12, FontStyleEnum.NORMAL, FontWeightEnum.BOLD,
                FontDecorationEnum.NONE);
        TextSymbol textSymbol = new TextSymbol(0, 0, 0, color, backgroundColor, borderLineColor,
                VerticalAlignmentEnum.BOTTOM, HorizontalAlignmentEnum.LEFT, false, font);
        Label polygonLabel = new PolygonLabel(PolygonLabelPlacementEnum.ALWAYS_HORIZONTAL,
                "[NAME]", false, textSymbol, 0, 0);
        String json = getJson(polygonLabel);
        assertTrue(validateJSON(json, "gsr/1.0/label.json"));
    }

    @Test
    public void testLabelInfoJsonSchema() throws Exception {
        List<Label> labels = new ArrayList<>();
        int[] color = { 78, 78, 78, 255 };
        int[] lineColor = { 78, 78, 78, 255 };
        int[] polygonColor = { 78, 78, 78, 255 };
        int[] backgroundColor = null;
        int[] borderLineColor = null;
        Font pointFont = new Font("Arial", 12, FontStyleEnum.NORMAL, FontWeightEnum.BOLD,
                FontDecorationEnum.NONE);
        Font lineFont = new Font("Arial", 12, FontStyleEnum.NORMAL, FontWeightEnum.BOLD,
                FontDecorationEnum.NONE);
        Font polygonFont = new Font("Arial", 12, FontStyleEnum.NORMAL, FontWeightEnum.BOLD,
                FontDecorationEnum.NONE);
        TextSymbol pointTextSymbol = new TextSymbol(0, 0, 0, color, backgroundColor,
                borderLineColor, VerticalAlignmentEnum.BOTTOM, HorizontalAlignmentEnum.LEFT, false,
                pointFont);
        Label pointLabel = new PointLabel(PointLabelPlacementEnum.ABOVE_RIGHT, "[NAME]", false,
                pointTextSymbol, 0, 0);
        TextSymbol lineTextSymbol = new TextSymbol(0, 0, 0, lineColor, backgroundColor,
                borderLineColor, VerticalAlignmentEnum.BOTTOM, HorizontalAlignmentEnum.LEFT, false,
                lineFont);
        Label lineLabel = new LineLabel(LineLabelPlacementEnum.ABOVE_BEFORE, "[NAME]", false,
                lineTextSymbol, 0, 0);
        TextSymbol polygonTextSymbol = new TextSymbol(0, 0, 0, polygonColor, backgroundColor,
                borderLineColor, VerticalAlignmentEnum.BOTTOM, HorizontalAlignmentEnum.LEFT, false,
                polygonFont);
        Label polygonLabel = new PolygonLabel(PolygonLabelPlacementEnum.ALWAYS_HORIZONTAL,
                "[TAG]", false, polygonTextSymbol, 0, 0);
        labels.add(pointLabel);
        labels.add(lineLabel);
        labels.add(polygonLabel);
        String json = getJson(labels);
        assertTrue(validateJSON(json, "gsr/1.0/labelInfo.json"));
    }

}
