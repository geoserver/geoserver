/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.renderer;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.gsr.JsonSchemaTest;
import org.geoserver.gsr.model.symbol.Outline;
import org.geoserver.gsr.model.symbol.SimpleFillSymbol;
import org.geoserver.gsr.model.symbol.SimpleFillSymbolEnum;
import org.geoserver.gsr.model.symbol.SimpleLineSymbol;
import org.geoserver.gsr.model.symbol.SimpleLineSymbolEnum;
import org.geoserver.gsr.model.symbol.SimpleMarkerSymbol;
import org.geoserver.gsr.model.symbol.SimpleMarkerSymbolEnum;
import org.geoserver.gsr.model.symbol.Symbol;
import org.junit.Test;

public class RendererSchemaTest extends JsonSchemaTest {

    @Test
    public void testSimpleRendererJsonSchema() throws Exception {

        int[] color = {255, 0, 0, 255};
        int[] outlinecolor = {0, 0, 0, 255};
        Outline outline = new Outline(outlinecolor, 1);
        SimpleMarkerSymbol sms =
                new SimpleMarkerSymbol(SimpleMarkerSymbolEnum.esriSMSCircle, color, 5, 0, 0, 0, outline);
        SimpleRenderer renderer = new SimpleRenderer(sms, "", "");
        String json = getJson(renderer);
        assertTrue(validateJSON(json, "/gsr/1.0/simpleRenderer.json"));
    }

    @Test
    public void testUniqueValueRendererJsonSchema() throws Exception {
        int[] color = {130, 130, 130, 255};
        Symbol defaultSymbol = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, color, 1);
        int[] color1 = {76, 0, 163, 255};
        Symbol symbol1 = new SimpleLineSymbol(SimpleLineSymbolEnum.DASH, color1, 1);
        int[] color2 = {115, 76, 0, 255};
        Symbol symbol2 = new SimpleLineSymbol(SimpleLineSymbolEnum.DOT, color2, 1);
        List<UniqueValueInfo> uniqueValueInfos = new ArrayList<>();
        UniqueValueInfo valueInfo1 = new UniqueValueInfo("1", "Duct Bank", "Duct Bank description", symbol1);
        UniqueValueInfo valueInfo2 = new UniqueValueInfo("2", "Trench", "Trench description", symbol2);
        uniqueValueInfos.add(valueInfo1);
        uniqueValueInfos.add(valueInfo2);
        UniqueValueRenderer renderer =
                new UniqueValueRenderer("SubtypeCD", null, null, ", ", defaultSymbol, "", uniqueValueInfos);
        String json = getJson(renderer);
        assertTrue(validateJSON(json, "/gsr/1.0/uniqueValueRenderer.json"));
    }

    @Test
    public void testClassBreaksRendererJsonSchema() throws Exception {
        int[] outlineColor1 = {110, 110, 110, 255};
        int[] outlineColor2 = {110, 110, 110, 255};
        SimpleLineSymbol outline1 = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, outlineColor1, 0.4);
        SimpleLineSymbol outline2 = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, outlineColor2, 0.4);
        int[] color1 = {236, 252, 204, 255};
        int[] color2 = {218, 240, 158, 255};
        Symbol symbol1 = new SimpleFillSymbol(SimpleFillSymbolEnum.SOLID, color1, outline1);
        Symbol symbol2 = new SimpleFillSymbol(SimpleFillSymbolEnum.SOLID, color2, outline2);
        ClassBreakInfo classBreakInfo1 = new ClassBreakInfo(null, 1000, "10.0 - 1000.0000", "10 to 1000", symbol1);
        ClassBreakInfo classBreakInfo2 =
                new ClassBreakInfo(null, 5000, "1000.00001 - 5000.0000", "1000 to 5000", symbol2);

        List<ClassBreakInfo> classBreakInfos = new ArrayList<>();
        classBreakInfos.add(classBreakInfo1);
        classBreakInfos.add(classBreakInfo2);
        ClassBreaksRenderer renderer = new ClassBreaksRenderer("Shape.area", 10.3906320193541, classBreakInfos);
        String json = getJson(renderer);
        assertTrue(validateJSON(json, "/gsr/1.0/classBreaksRenderer.json"));
    }
}
