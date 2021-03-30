/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geotools.data.FeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.w3c.dom.Document;

public class GetMapCallbackTest extends WMSDimensionsTestSupport {

    private GetMap getMap;

    @Before
    public void cleanupCallbacks() {
        getMap = applicationContext.getBean(GetMap.class);
        getMap.setGetMapCallbacks(Collections.emptyList());
    }

    @Test
    public void testStandardWorkflow() throws Exception {
        TestCallback callback = new TestCallback();
        getMap.setGetMapCallbacks(Arrays.asList(callback));

        // request a layer group with two layers
        Document dom = getAsDOM("wms?request=reflect&layers=nature&format=rss");
        assertXpathExists("rss/channel/title[text() = 'cite:Lakes,cite:Forests']", dom);

        assertEquals(1, callback.requests.size());
        assertEquals(1, callback.mapContentsInited.size());
        assertEquals(2, callback.layers.size());
        assertEquals(1, callback.mapContents.size());
        assertEquals(1, callback.maps.size());
        assertEquals(0, callback.exceptions.size());
    }

    @Test
    public void testBreakRequest() throws Exception {
        final String message = "This layer is not allowed";
        TestCallback callback =
                new TestCallback() {
                    @Override
                    public Layer beforeLayer(WMSMapContent content, Layer layer) {
                        throw new RuntimeException(message);
                    }
                };
        getMap.setGetMapCallbacks(Arrays.asList(callback));

        // request a layer group with two layers
        Document dom = getAsDOM("wms?request=reflect&layers=nature&format=rss&version=1.1.0");
        // print(dom);
        assertXpathExists("/ServiceExceptionReport", dom);

        assertEquals(1, callback.requests.size());
        assertEquals(1, callback.mapContentsInited.size());
        assertEquals(0, callback.layers.size());
        assertEquals(0, callback.mapContents.size());
        assertEquals(0, callback.maps.size());
        assertEquals(1, callback.exceptions.size());
        assertEquals(message, callback.exceptions.get(0).getMessage());
    }

    @Test
    public void testAddLayer() throws Exception {
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(getLayerId(MockData.BRIDGES));
        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                ft.getFeatureSource(null, null);
        Style style = getCatalog().getStyleByName("point").getStyle();
        final FeatureLayer layer = new FeatureLayer(fs, style);
        layer.setTitle("extra");
        TestCallback callback =
                new TestCallback() {
                    @Override
                    public WMSMapContent beforeRender(WMSMapContent mapContent) {
                        mapContent.addLayer(layer);
                        return super.beforeRender(mapContent);
                    }
                };
        getMap.setGetMapCallbacks(Arrays.asList(callback));

        // request a layer group with two layers
        Document dom = getAsDOM("wms?request=reflect&layers=nature&format=rss&version=1.1.0");
        // print(dom);
        assertXpathExists("rss/channel/title[text() = 'cite:Lakes,cite:Forests,extra']", dom);

        assertEquals(1, callback.requests.size());
        assertEquals(1, callback.mapContentsInited.size());
        assertEquals(3, callback.layers.size());
        assertEquals(1, callback.mapContents.size());
        assertEquals(1, callback.maps.size());
        assertEquals(0, callback.exceptions.size());

        assertEquals(layer, callback.layers.get(2));
    }

    @Test
    public void testRemoveLayer() throws Exception {
        TestCallback callback =
                new TestCallback() {
                    @Override
                    public Layer beforeLayer(WMSMapContent content, Layer layer) {
                        if ("cite:Lakes".equals(layer.getTitle())) {
                            return null;
                        } else {
                            return super.beforeLayer(content, layer);
                        }
                    }
                };
        getMap.setGetMapCallbacks(Arrays.asList(callback));

        // request a layer group with two layers
        Document dom = getAsDOM("wms?request=reflect&layers=nature&format=rss&version=1.1.0");
        // print(dom);
        assertXpathExists("rss/channel/title[text() = 'cite:Forests']", dom);

        assertEquals(1, callback.requests.size());
        assertEquals(1, callback.mapContentsInited.size());
        assertEquals(1, callback.layers.size());
        assertEquals(1, callback.mapContents.size());
        assertEquals(1, callback.maps.size());
        assertEquals(0, callback.exceptions.size());

        assertEquals("cite:Forests", callback.layers.get(0).getTitle());
    }

    private class TestCallback implements GetMapCallback {

        private List<GetMapRequest> requests = new ArrayList<>();

        private List<WMSMapContent> mapContentsInited = new ArrayList<>();

        private List<Layer> layers = new ArrayList<>();

        private List<WMSMapContent> mapContents = new ArrayList<>();

        private List<WebMap> maps = new ArrayList<>();

        private List<Throwable> exceptions = new ArrayList<>();

        @Override
        public synchronized GetMapRequest initRequest(GetMapRequest request) {
            requests.add(request);
            return request;
        }

        @Override
        public synchronized void initMapContent(WMSMapContent mapContent) {
            mapContentsInited.add(mapContent);
        }

        @Override
        public synchronized Layer beforeLayer(WMSMapContent content, Layer layer) {
            layers.add(layer);
            return layer;
        }

        @Override
        public synchronized WMSMapContent beforeRender(WMSMapContent mapContent) {
            mapContents.add(mapContent);
            return mapContent;
        }

        @Override
        public synchronized WebMap finished(WebMap map) {
            maps.add(map);
            return map;
        }

        @Override
        public synchronized void failed(Throwable t) {
            exceptions.add(t);
        }
    }
}
