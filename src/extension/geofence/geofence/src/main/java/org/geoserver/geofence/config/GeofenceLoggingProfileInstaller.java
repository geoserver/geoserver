/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.config;

import org.geoserver.catalog.Catalog;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** Copies the {@code GEOFENCE_LOGGING} profile into the data directory on startup, if not already present. */
@Component
public class GeofenceLoggingProfileInstaller implements ApplicationListener<ContextLoadedEvent> {

    @Autowired
    private Catalog catalog;

    @Override
    public void onApplicationEvent(ContextLoadedEvent event) {
        GeoServerResourceLoader loader = catalog.getResourceLoader();
        LoggingUtils.checkBuiltInLoggingConfiguration(loader, "GEOFENCE_LOGGING");
    }
}
