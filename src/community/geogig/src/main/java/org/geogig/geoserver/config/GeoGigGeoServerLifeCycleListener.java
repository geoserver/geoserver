/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import org.geoserver.config.impl.GeoServerLifecycleHandler;

public class GeoGigGeoServerLifeCycleListener implements GeoServerLifecycleHandler {

    /** Called by geoserver to flush caches */
    @Override
    public void onReset() {
        RepositoryManager.close();
    }

    @Override
    public void onDispose() {
        RepositoryManager.close();
    }

    @Override
    public void onReload() {
        //
    }

    @Override
    public void beforeReload() {
        //
    }
}
