/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import java.util.logging.Logger;

import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.gwc.GWC;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.InitializingBean;

/**
 * GeoServer life cycle listener that reloads the gwc configuration
 * <p>
 * Note: this class implements {@link InitializingBean} to force spring to create it once its
 * {@link GWC} dependency is ready. Otherwise it may not be created until the application is in the
 * process of shutting down, which causes an exception as the GWC instance cannot be provided by
 * then (see GEOS-6468).
 */
public class GWCLifeCycleHandler implements GeoServerLifecycleHandler, InitializingBean {

    private static final Logger LOGGER = Logging.getLogger(GWCLifeCycleHandler.class);

    private final GWC mediator;

    public GWCLifeCycleHandler(GWC mediator) {
        this.mediator = mediator;
        LOGGER.fine("GWC life cycle handler created.");
    }

    public void beforeReload() {
        // nothing to do
    }

    @Override
    public void onReload() {
        mediator.reload();
    }

    @Override
    public void onReset() {
        mediator.reset();
    }

    @Override
    public void onDispose() {
        onReset();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Do nothing, just forcing the bean to be created
    }

}
