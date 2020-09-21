/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cog;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;

/** Simple initializer to populate the Global COG settings on first usage */
public class CogSettingsInitializer implements GeoServerInitializer {

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        // Add a new Element to the metadata map
        GeoServerInfo global = geoServer.getGlobal();
        MetadataMap metadata = global.getSettings().getMetadata();
        if (!metadata.containsKey(CogSettings.COG_SETTINGS_KEY)) {
            metadata.put(CogSettings.COG_SETTINGS_KEY, new CogSettings());
            geoServer.save(global);
        }
    }
}
