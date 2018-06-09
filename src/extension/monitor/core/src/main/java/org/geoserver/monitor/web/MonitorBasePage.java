/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.web;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * Base page for monitor web pages.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MonitorBasePage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 5400909938411833603L;

    protected Monitor getMonitor() {
        return getGeoServerApplication().getBeanOfType(Monitor.class);
    }

    protected MonitorDAO getMonitorDAO() {
        return getMonitor().getDAO();
    }
}
