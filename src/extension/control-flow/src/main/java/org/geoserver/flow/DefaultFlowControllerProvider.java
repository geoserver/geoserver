/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.flow.config.DefaultControlFlowConfigurator;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;

/**
 * A provider that always returns the same set of flow control rules, regardless of the current
 * request, based on the list provided by a ControlFlowConfigurator
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DefaultFlowControllerProvider implements FlowControllerProvider {

    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    ControlFlowConfigurator configurator;

    List<FlowController> controllers = Collections.emptyList();

    private long timeout;

    public DefaultFlowControllerProvider(ApplicationContext applicationContext) {
        // look for a ControlFlowConfigurator in the application context, if none is found, use the
        // default one
        configurator = GeoServerExtensions.bean(ControlFlowConfigurator.class, applicationContext);
        if (configurator == null) {
            configurator = new DefaultControlFlowConfigurator();
        }
        initControllers();
    }

    public DefaultFlowControllerProvider(ControlFlowConfigurator configurator) {
        this.configurator = configurator;
        initControllers();
    }

    private void initControllers() {
        checkConfiguration();
        if (controllers.size() == 0) {
            LOGGER.info("Control-flow inactive, there are no configured rules");
        }
    }

    @Override
    public List<FlowController> getFlowControllers(Request request) throws Exception {
        checkConfiguration();
        return controllers;
    }

    @Override
    public long getTimeout(Request request) {
        return timeout;
    }

    private void checkConfiguration() {
        // check if we need to rebuild the flow controller list
        if (configurator.isStale()) {
            // be careful, as the configuration can be read on demand, it'd not be uncommon that
            // multiple requests come at once when the config file changed
            synchronized (configurator) {
                if (configurator.isStale()) {
                    reloadConfiguration();
                }
            }
        }
    }

    /** Reloads the flow controller list and replaces the existing ones */
    void reloadConfiguration() {
        try {
            List<FlowController> newControllers =
                    new ArrayList<FlowController>(configurator.buildFlowControllers());
            Collections.sort(newControllers, new ControllerPriorityComparator());
            controllers = newControllers;
            int controllersCount = controllers.size();
            if (controllersCount > 0) {
                LOGGER.info("Control-flow active with " + controllersCount + " flow controllers");
            } else {
                LOGGER.info("Control-flow inactive, there are no configured rules");
            }
            timeout = configurator.getTimeout();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurerd during flow controllers reconfiguration");
        }
    }
}
