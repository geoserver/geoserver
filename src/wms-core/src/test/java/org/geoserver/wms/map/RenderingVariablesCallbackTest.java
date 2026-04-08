/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geoserver.wms.RenderingVariables.WMS_BBOX;
import static org.geoserver.wms.RenderingVariables.WMS_CRS;
import static org.geoserver.wms.RenderingVariables.WMS_HEIGHT;
import static org.geoserver.wms.RenderingVariables.WMS_SCALE_DENOMINATOR;
import static org.geoserver.wms.RenderingVariables.WMS_SRS;
import static org.geoserver.wms.RenderingVariables.WMS_WIDTH;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.wms.RenderingVariablesCallback;
import org.geoserver.wms.WMSMapContent;
import org.geotools.api.data.Query;
import org.geotools.api.referencing.FactoryException;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.filter.function.EnvFunction;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.CRS;
import org.geotools.styling.StyleBuilder;
import org.geotools.util.factory.Hints;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class RenderingVariablesCallbackTest {

    private static final RenderingVariablesCallback callback = new RenderingVariablesCallback();

    @Before
    public void init() throws FactoryException {
        EnvFunction.setLocalValue(WMS_BBOX, ReferencedEnvelope.rect(-180, -90, 10, 5));
        EnvFunction.setLocalValue(WMS_CRS, CRS.decode("EPSG:4326"));
        EnvFunction.setLocalValue(WMS_SRS, "EPSG:4326");
        EnvFunction.setLocalValue(WMS_WIDTH, 5);
        EnvFunction.setLocalValue(WMS_HEIGHT, 4);
        EnvFunction.setLocalValue(WMS_SCALE_DENOMINATOR, 1_000_000);
    }

    @Test
    public void testEnvParamsAreSet() {
        final DefaultFeatureCollection fc = new DefaultFeatureCollection();
        StyleBuilder sb = new StyleBuilder();
        FeatureLayer layer = new FeatureLayer(fc, sb.createStyle());
        Query query = new Query();
        query.setHints(new Hints());
        layer.setQuery(query);

        WMSMapContent wmsMapContent = new WMSMapContent();
        wmsMapContent.addLayer(layer);
        callback.beforeRender(wmsMapContent);

        Map<String, String> virtualParams =
                (Map<String, String>) layer.getQuery().getHints().get(Hints.VIRTUAL_TABLE_PARAMETERS);
        assertEquals("-180.000000,-90.000000,-170.000000,-85.000000", virtualParams.get(WMS_BBOX));
        assertEquals("EPSG:4326", virtualParams.get(WMS_SRS));
        assertEquals("5", virtualParams.get(WMS_WIDTH));
        assertEquals("4", virtualParams.get(WMS_HEIGHT));
        assertEquals("1000000", virtualParams.get(WMS_SCALE_DENOMINATOR));
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
        paramMap.put(WMS_SRS, "EPSG:3857");
        paramMap.put(WMS_WIDTH, "10");
        paramMap.put(WMS_HEIGHT, "8");
        paramMap.put(WMS_SCALE_DENOMINATOR, "2000000");

        hints.put(Hints.VIRTUAL_TABLE_PARAMETERS, paramMap);
        query.setHints(hints);
        layer.setQuery(query);

        WMSMapContent wmsMapContent = new WMSMapContent();
        wmsMapContent.addLayer(layer);
        callback.beforeRender(wmsMapContent);

        Map<String, String> virtualParams =
                (Map<String, String>) layer.getQuery().getHints().get(Hints.VIRTUAL_TABLE_PARAMETERS);
        assertEquals("-100,-80,100,80", virtualParams.get(WMS_BBOX));
        assertEquals("EPSG:3857", virtualParams.get(WMS_SRS));
        assertEquals("10", virtualParams.get(WMS_WIDTH));
        assertEquals("8", virtualParams.get(WMS_HEIGHT));
        assertEquals("2000000", virtualParams.get(WMS_SCALE_DENOMINATOR));
    }
}
