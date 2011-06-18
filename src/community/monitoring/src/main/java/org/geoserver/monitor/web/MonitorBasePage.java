package org.geoserver.monitor.web;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * Base page for monitor web pages.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class MonitorBasePage extends GeoServerSecuredPage {


    protected Monitor getMonitor() {
        return getGeoServerApplication().getBeanOfType(Monitor.class);
    }
    
    protected MonitorDAO getMonitorDAO() {
        return getMonitor().getDAO();
    }
}
