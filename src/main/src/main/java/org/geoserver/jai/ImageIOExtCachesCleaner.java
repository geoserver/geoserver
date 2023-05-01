/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jai;

import it.geosolutions.imageio.core.ExtCaches;
import org.geoserver.config.impl.GeoServerLifecycleHandler;

/** Cleans up eventual caches ImageI/O-Ext may be holding onto (e.g., the COG headers cache) */
public class ImageIOExtCachesCleaner implements GeoServerLifecycleHandler {

    @Override
    public void onReset() {
        ExtCaches.clean();
    }

    @Override
    public void onDispose() {
        // nothing to do
    }

    @Override
    public void beforeReload() {
        // nothing to do
    }

    @Override
    public void onReload() {
        ExtCaches.clean();
    }
}
