/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.label;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opengeo.gsr.JsonSchemaTest;
import org.opengeo.gsr.core.font.Font;
import org.opengeo.gsr.core.font.FontDecorationEnum;
import org.opengeo.gsr.core.font.FontStyleEnum;
import org.opengeo.gsr.core.font.FontWeightEnum;
import org.opengeo.gsr.core.label.Label;
import org.opengeo.gsr.core.label.LineLabel;
import org.opengeo.gsr.core.label.LineLabelPlacementEnum;
import org.opengeo.gsr.core.label.PointLabel;
import org.opengeo.gsr.core.label.PointLabelPlacementEnum;
import org.opengeo.gsr.core.label.PolygonLabel;
import org.opengeo.gsr.core.label.PolygonLabelPlacementEnum;
import org.opengeo.gsr.core.symbol.HorizontalAlignmentEnum;
import org.opengeo.gsr.core.symbol.TextSymbol;
import org.opengeo.gsr.core.symbol.VerticalAlignmentEnum;

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
        List<Label> labels = new ArrayList<Label>();
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
