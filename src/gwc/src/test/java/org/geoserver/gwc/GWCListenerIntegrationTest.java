package org.geoserver.gwc;

import static org.geoserver.gwc.GWC.*;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.layer.TileLayerDispatcher;

public class GWCListenerIntegrationTest extends GeoServerTestSupport {

    @Override
    protected boolean useLegacyDataDirectory() {
        return false;
    }
    
    public void testRemoveLayerAfterReload() throws Exception {
        Catalog cat = getCatalog();
        TileLayerDispatcher tld = GeoWebCacheExtensions.bean(TileLayerDispatcher.class);
        
        LayerInfo li = cat.getLayerByName(super.getLayerId(MockData.MPOINTS));
        String layerName = tileLayerName(li);
    
        assertNotNull(tld.getTileLayer(layerName));
    
        // force reload
        getGeoServer().reload();
        
        // now remove the layer and check it has been removed from GWC as well
        cat.remove(li);
        try {
            tld.getTileLayer(layerName);
            fail("Layer should not exist");
        } catch (GeoWebCacheException gwce) {
            // fine
        }
    }
}
