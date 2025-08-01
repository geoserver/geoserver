/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2025, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wms.map;

import static org.geoserver.wms.RenderingVariablesCallback.WMS_BBOX;
import static org.geoserver.wms.RenderingVariablesCallback.WMS_CRS;
import static org.geoserver.wms.RenderingVariablesCallback.WMS_HEIGHT;
import static org.geoserver.wms.RenderingVariablesCallback.WMS_SCALE_DENOMINATOR;
import static org.geoserver.wms.RenderingVariablesCallback.WMS_SRS;
import static org.geoserver.wms.RenderingVariablesCallback.WMS_WIDTH;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.wms.RenderingVariablesCallback;
import org.geotools.api.data.Query;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.filter.function.EnvFunction;
import org.geotools.map.FeatureLayer;
import org.geotools.styling.StyleBuilder;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class RenderingVariablesCallbackTest {

    private static final RenderingVariablesCallback callback = new RenderingVariablesCallback();

    @Before
    public void init() {
        EnvFunction.setLocalValue(WMS_BBOX, "-180,-90,180,90");
        EnvFunction.setLocalValue(WMS_CRS, "EPSG:4836");
        EnvFunction.setLocalValue(WMS_SRS, "EPSG:4836");
        EnvFunction.setLocalValue(WMS_WIDTH, "5");
        EnvFunction.setLocalValue(WMS_HEIGHT, "4");
        EnvFunction.setLocalValue(WMS_SCALE_DENOMINATOR, "1_000_000");
    }

    @Test
    public void testEnvParamsAreSet() {
        final DefaultFeatureCollection fc = new DefaultFeatureCollection();
        StyleBuilder sb = new StyleBuilder();
        FeatureLayer layer = new FeatureLayer(fc, sb.createStyle());
        Query query = new Query();
        query.setHints(new Hints());
        layer.setQuery(query);

        callback.beforeLayer(null, layer);

        Map<String, String> virtualParams =
                (Map<String, String>) layer.getQuery().getHints().get(Hints.VIRTUAL_TABLE_PARAMETERS);
        assertEquals("-180,-90,180,90", virtualParams.get(WMS_BBOX));
        assertEquals("EPSG:4836", virtualParams.get(WMS_CRS));
        assertEquals("EPSG:4836", virtualParams.get(WMS_SRS));
        assertEquals("5", virtualParams.get(WMS_WIDTH));
        assertEquals("4", virtualParams.get(WMS_HEIGHT));
        assertEquals("1_000_000", virtualParams.get(WMS_SCALE_DENOMINATOR));
    }

    @Test
    public void testEnvParamsAreNotOverwritten() {
        final DefaultFeatureCollection fc = new DefaultFeatureCollection();
        StyleBuilder sb = new StyleBuilder();
        FeatureLayer layer = new FeatureLayer(fc, sb.createStyle());
        Query query = new Query();
        Hints hints = new Hints();
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put(WMS_BBOX, "-100,-80,100,80");
        paramMap.put(WMS_CRS, "EPSG:3857");
        paramMap.put(WMS_SRS, "EPSG:3857");
        paramMap.put(WMS_WIDTH, "10");
        paramMap.put(WMS_HEIGHT, "8");
        paramMap.put(WMS_SCALE_DENOMINATOR, "2_000_000");

        hints.put(Hints.VIRTUAL_TABLE_PARAMETERS, paramMap);
        query.setHints(hints);
        layer.setQuery(query);

        callback.beforeLayer(null, layer);

        Map<String, String> virtualParams =
                (Map<String, String>) layer.getQuery().getHints().get(Hints.VIRTUAL_TABLE_PARAMETERS);
        assertEquals("-100,-80,100,80", virtualParams.get(WMS_BBOX));
        assertEquals("EPSG:3857", virtualParams.get(WMS_CRS));
        assertEquals("EPSG:3857", virtualParams.get(WMS_SRS));
        assertEquals("10", virtualParams.get(WMS_WIDTH));
        assertEquals("8", virtualParams.get(WMS_HEIGHT));
        assertEquals("2_000_000", virtualParams.get(WMS_SCALE_DENOMINATOR));
    }
}
