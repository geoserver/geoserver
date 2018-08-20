/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.geoserver.gwc.GWC;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestSupport;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.geowebcache.layer.TileLayer;
import org.junit.After;
import org.junit.Test;

public class CachedLayerProviderTest extends GeoServerTestSupport {

    @After
    public void testQuotaDisabledWithSystemVariable() throws IllegalAccessException {
        DiskQuotaMonitor monitor = GeoServerExtensions.bean(DiskQuotaMonitor.class);
        // the field is initialized once based on system variable, we use reflection
        // to force it to a different value and set it back where it was
        Field enabledField = FieldUtils.getField(DiskQuotaMonitor.class, "diskQuotaEnabled", true);
        try {
            FieldUtils.writeField(enabledField, monitor, false, true);

            CachedLayerProvider provider = new CachedLayerProvider();
            List<TileLayer> layers = provider.getItems();
            for (TileLayer tileLayer : layers) {
                // we are not returning the values from the quota subsystem, they are not up to date
                // anyways
                assertNull(CachedLayerProvider.QUOTA_USAGE.getPropertyValue(tileLayer));
            }
        } finally {
            FieldUtils.writeField(enabledField, monitor, true, true);
        }
    }

    @Test
    public void testQuotaEnabled()
            throws ConfigurationException, IOException, InterruptedException {
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
    public void testQuotaDisabled()
            throws ConfigurationException, IOException, InterruptedException {
        GWC gwc = GWC.get();
        DiskQuotaConfig config = gwc.getDiskQuotaConfig();
        config.setEnabled(false);
        gwc.saveDiskQuotaConfig(config, null);

        CachedLayerProvider provider = new CachedLayerProvider();
        List<TileLayer> layers = provider.getItems();
        for (TileLayer tileLayer : layers) {
            // we are not returning the values from the quota subsystem, they are not up to date
            // anyways
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
