package org.opengeo.gsr.core.symbol;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opengeo.gsr.JsonSchemaTest;

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
        Label label = new PointLabel(PointLabelPlacementEnum.ABOVE_RIGHT, "[NAME]", false,
                textSymbol, 0, 0);
        String json = getJson(label);
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
        Label label = new LineLabel(LineLabelPlacementEnum.ABOVE_BEFORE, "[NAME]", false,
                textSymbol, 0, 0);
        String json = getJson(label);
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
        Label label = new PolygonLabel(PolygonLabelPlacementEnum.ALWAYS_HORIZONTAL, "[NAME]",
                false, textSymbol, 0, 0);
        String json = getJson(label);
        assertTrue(validateJSON(json, "gsr/1.0/label.json"));
    }

}
