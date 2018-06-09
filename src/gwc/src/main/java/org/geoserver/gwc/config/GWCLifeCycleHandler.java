/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.gwc.GWC;

/** GeoServer life cycle listener that reloads the gwc configuration */
public class GWCLifeCycleHandler implements GeoServerLifecycleHandler {

    public GWCLifeCycleHandler() {
        // nothing to do
    }

    public void beforeReload() {
        // nothing to do
    }

    @Override
    public void onReload() {
        GWC.tryReload();
    }

    @Override
    public void onReset() {
        GWC.tryReset();
    }

    @Override
    public void onDispose() {
        onReset();
    }
}
