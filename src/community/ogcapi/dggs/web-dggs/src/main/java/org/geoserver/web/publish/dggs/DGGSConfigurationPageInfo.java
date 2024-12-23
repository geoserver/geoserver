/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish.dggs;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.publish.LayerConfigurationPanelInfo;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.util.logging.Logging;

/** Configuration page for DGGS layers, makes sure the layer is a feature type and the store is a DGGS store. */
public class DGGSConfigurationPageInfo extends LayerConfigurationPanelInfo {
    private static final Logger LOGGER = Logging.getLogger(DGGSConfigurationPageInfo.class);

    @Override
    public boolean canHandle(PublishedInfo published) {
        if (!(published instanceof LayerInfo)) return false;
        LayerInfo li = (LayerInfo) published;
        if (!(li.getResource() instanceof FeatureTypeInfo)) return false;
        FeatureTypeInfo fti = (FeatureTypeInfo) li.getResource();
        try {
            return fti.getStore().getDataStore(null) instanceof DGGSStore;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to connect to store", e);
        }

        return false;
    }
}
