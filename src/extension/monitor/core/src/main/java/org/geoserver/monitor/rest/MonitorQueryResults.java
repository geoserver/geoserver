package org.geoserver.monitor.rest;

import java.util.List;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.RequestData;

/**
 * Wrapper class for REST monitor request results
 */
public class MonitorQueryResults {

    Object result;
    String[] fields;
    Monitor monitor;
    
    public MonitorQueryResults(Object result, String[] fields, Monitor monitor) {
        this.result = result;
        this.fields = fields;
        this.monitor = monitor;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    /**
     * Monitor results: Query, list of RequestData, or single RequestData.
     * @return
     */
    public Object getResult() {
        return result;
    }

    public String[] getFields() {
        return fields;
    }

    
}
