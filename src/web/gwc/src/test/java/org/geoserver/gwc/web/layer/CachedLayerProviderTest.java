package org.geoserver.gwc.web.layer;

import java.io.IOException;
import java.util.List;

import org.geoserver.gwc.GWC;
import org.geoserver.test.GeoServerTestSupport;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.layer.TileLayer;
import org.junit.Test;

public class CachedLayerProviderTest extends GeoServerTestSupport {

    @Test
    public void testQuotaEnabled() throws ConfigurationException, IOException, InterruptedException {
        GWC gwc = GWC.get();
        DiskQuotaConfig config = gwc.getDiskQuotaConfig();
        config.setEnabled(true);
        gwc.saveDiskQuotaConfig(config, null);
        
        CachedLayerProvider provider = new CachedLayerProvider();
        List<TileLayer> layers = provider.getItems();
        for (TileLayer tileLayer : layers) {
            // we are returning the values from the quota subsystem
            assertNotNull(CachedLayerProvider.QUOTA_USAGE.getPropertyValue(tileLayer));
        }
    }
    
    @Test
    public void testQuotaDisabled() throws ConfigurationException, IOException, InterruptedException {
        GWC gwc = GWC.get();
        DiskQuotaConfig config = gwc.getDiskQuotaConfig();
        config.setEnabled(false);
        gwc.saveDiskQuotaConfig(config, null);
        
        CachedLayerProvider provider = new CachedLayerProvider();
        List<TileLayer> layers = provider.getItems();
        for (TileLayer tileLayer : layers) {
            // we are not returning the values from the quota subsystem, they are not up to date anyways
            assertNull(CachedLayerProvider.QUOTA_USAGE.getPropertyValue(tileLayer));
        }
    }
        
}
