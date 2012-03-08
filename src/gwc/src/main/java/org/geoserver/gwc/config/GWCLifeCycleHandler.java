/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.gwc.GWC;

/**
 * GeoServer life cycle listener that reloads the gwc configuration
 * 
 */
public class GWCLifeCycleHandler implements GeoServerLifecycleHandler {

    @Override
    public void onReload() {
        GWC mediator = GWC.get();
        mediator.reload();
    }

    @Override
    public void onReset() {
        // nothing to flush?
    }

    @Override
    public void onDispose() {
        // nothing to do, dispose is handled by GWC.destroy()
    }

}
