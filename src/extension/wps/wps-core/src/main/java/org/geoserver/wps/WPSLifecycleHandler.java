/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.geoserver.wps.WPSInitializer.lookupNewProcessGroups;

import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerLifecycleHandler;

/**
 * Life cycle listener for WPS.
 *
 * <p>Currently this instance will reload the process group list, registering new factories if
 * required on reload/reset.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class WPSLifecycleHandler implements GeoServerLifecycleHandler {

    GeoServer geoServer;

    public WPSLifecycleHandler(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public void onReset() {
        lookupNewProcessGroups(getWPS(), geoServer);
    }

    public void beforeReload() {
        // nothing to do
    }

    @Override
    public void onDispose() {}

    @Override
    public void onReload() {
        lookupNewProcessGroups(getWPS(), geoServer);
    }

    WPSInfo getWPS() {
        return geoServer.getService(WPSInfo.class);
    }
}
