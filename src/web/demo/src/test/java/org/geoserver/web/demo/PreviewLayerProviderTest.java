package org.geoserver.web.demo;

import javax.xml.namespace.QName;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;

public class PreviewLayerProviderTest extends GeoServerWicketTestSupport {

    public void testNonAdvertisedLayer() throws Exception {
        String layerId = getLayerId(MockData.BUILDINGS);
        LayerInfo layer = getCatalog().getLayerByName(layerId);
        try {
            // now you see me
            PreviewLayerProvider provider = new PreviewLayerProvider();
            PreviewLayer pl = getPreviewLayer(provider, MockData.BUILDINGS);
            assertNotNull(pl);
            
            // now you don't!
            layer.setAdvertised(false);
            getCatalog().save(layer);
            pl = getPreviewLayer(provider, MockData.BUILDINGS);
            assertNull(pl);
        } finally {
            layer.setAdvertised(true);
            getCatalog().save(layer);
        }
    }

    private PreviewLayer getPreviewLayer(PreviewLayerProvider provider, QName layer) {
        String layerId = getLayerId(layer);
        for (PreviewLayer pl : provider.getItems()) {
            if(pl.getName().equals(layerId)) {
                return pl; 
            }
        }
        return null;
    }
}
