package org.geoserver.gwc.web.layer;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import java.io.IOException;
import java.util.List;
import java.util.Set;

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
    
    @Test
    public void testAdvertised() {
        GWC oldGWC = GWC.get();
        GWC gwc = mock(GWC.class);
        GWC.set(gwc);
        // Adding a few Mocks for an Unadvertised Layer
        TileLayer l = mock(TileLayer.class);
        when(l.isAdvertised()).thenReturn(false);
        
        // Calculating the size of the Layers with the unadvertised one
        Set<String> tileLayerNames = gwc.getTileLayerNames();
        tileLayerNames.add("testUnAdvertised");
        // Real size of the Layer names Set
        int gwcSize = tileLayerNames.size() - 1;
        
        // Mocks for the GWC class
        when(gwc.getTileLayerNames()).thenReturn(tileLayerNames);
        when(gwc.getTileLayerByName("testUnAdvertised")).thenReturn(l);
        
        // Calculate the number of TileLayers found
        CachedLayerProvider provider = new CachedLayerProvider();
        int providerSize = provider.getItems().size();
        
        // Ensure that the two numbers are equal
        assertEquals(gwcSize, providerSize);
        
        // Set the old GWC
        GWC.set(oldGWC);
    }
        
}
