/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dynamic.legendgraphic;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.legendgraphic.GetLegendGraphicKvpReader;
import org.geotools.process.raster.DynamicColorMapTest;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.xml.styling.SLDTransformer;
import org.junit.Test;

public class DynamicGetLegendGraphicsCallbackTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        testData.addStyle(
                "style_rgb", "test-data/style_rgb.sld", DynamicColorMapTest.class, catalog);
        Map<LayerProperty, Object> properties = new HashMap<>();
        properties.put(LayerProperty.STYLE, "style_rgb");
        testData.addRasterLayer(
                new QName(MockData.DEFAULT_URI, "watertemp_dynamic", MockData.DEFAULT_PREFIX),
                "test-data/watertemp_dynamic.zip",
                null,
                properties,
                DynamicColorMapTest.class,
                catalog);
    }

    @Test
    public void testLegendExpasion() throws Exception {
        // manually parse a request
        GetLegendGraphicKvpReader requestReader =
                GeoServerExtensions.bean(GetLegendGraphicKvpReader.class);
        Map params = new KvpMap();
        params.put("VERSION", "1.0.0");
        params.put("REQUEST", "GetLegendGraphic");
        params.put("LAYER", "watertemp_dynamic");
        params.put("STYLE", "style_rgb");
        params.put("FORMAT", "image/png");
        GetLegendGraphicRequest getLegendGraphics =
                requestReader.read(new GetLegendGraphicRequest(), params, params);

        // setup to call the callback
        Service wmsService = (Service) GeoServerExtensions.bean("wms-1_1_1-ServiceDescriptor");
        Operation op =
                new Operation(
                        "getLegendGraphic", wmsService, null, new Object[] {getLegendGraphics});
        Request request = new Request();
        request.setKvp(params);
        request.setRawKvp(params);
        Dispatcher.REQUEST.set(request);
        DynamicGetLegendGraphicDispatcherCallback callback =
                GeoServerExtensions.bean(DynamicGetLegendGraphicDispatcherCallback.class);
        callback.operationDispatched(null, op);

        // get the style and check it has been transformed (we started with one having a
        // transformation, now
        // we have a static colormap)
        Style style = getLegendGraphics.getLegends().get(0).getStyle();
        FeatureTypeStyle fts = style.featureTypeStyles().get(0);
        assertNull(fts.getTransformation());
        RasterSymbolizer rs = (RasterSymbolizer) fts.rules().get(0).symbolizers().get(0);
        assertNotNull(rs.getColorMap());
    }

    void logStyle(Style style) {
        SLDTransformer tx = new SLDTransformer();
        tx.setIndentation(2);
        try {
            tx.transform(style, System.out);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}
