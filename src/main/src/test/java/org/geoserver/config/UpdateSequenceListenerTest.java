package org.geoserver.config;

import org.geoserver.test.GeoServerTestSupport;

public class UpdateSequenceListenerTest extends GeoServerTestSupport {

    public void testCatalogUpdates() {
        long updateSequence = getGeoServer().getGlobal().getUpdateSequence();
        
        // remove one layer
        getCatalog().remove(getCatalog().getLayers().get(0));
        
        long newUpdateSequence = getGeoServer().getGlobal().getUpdateSequence();
        assertTrue(newUpdateSequence > updateSequence);
    }
    
    public void testServiceUpdates() {
        GeoServerInfo global = getGeoServer().getGlobal();
        long updateSequence = global.getUpdateSequence();
        
        // change a flag in the config
        global.setVerbose(true);
        getGeoServer().save(global);
        
        
        long newUpdateSequence = getGeoServer().getGlobal().getUpdateSequence();
        assertTrue(newUpdateSequence > updateSequence);
    }
}
