/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geotools.util.logging.Logging;

public class MonitorInitializer implements GeoServerInitializer {

    static Logger LOGGER = Logging.getLogger(Monitor.class);

    Monitor monitor;

    public MonitorInitializer(Monitor monitor) {
        this.monitor = monitor;
    }

    public void initialize(GeoServer geoServer) throws Exception {
        monitor.setServer(geoServer);
    }
}
