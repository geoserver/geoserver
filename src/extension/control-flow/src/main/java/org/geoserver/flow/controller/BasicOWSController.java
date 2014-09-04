/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import org.geoserver.ows.Request;

/**
 * A flow controller that can categorize requests by service, method and output format
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class BasicOWSController extends SingleQueueFlowController {
    String service;

    String method;

    String outputFormat;

    public BasicOWSController(String service, int queueSize) {
        this(service, null, null, queueSize);
    }

    public BasicOWSController(String service, String method, int queueSize) {
        this(service, method, null, queueSize);
    }

    public BasicOWSController(String service, String method, String outputFormat, int queueSize) {
        super(queueSize);
        this.service = service;
        this.method = method;
        this.outputFormat = outputFormat;

        if (service == null)
            throw new IllegalArgumentException("Invalid OWS definition, service cannot be non null");
        else if (method == null && outputFormat != null)
            throw new IllegalArgumentException(
                    "Invalid OWS definition, output format cannot be null if method is not provided");
    }

    @Override
    boolean matchesRequest(Request request) {
        if (!service.equalsIgnoreCase(request.getService()))
            return false;

        if (method == null)
            return true;
        else if (!method.equalsIgnoreCase(request.getRequest()))
            return false;

        if (outputFormat == null)
            return true;
        else if (!outputFormat.equalsIgnoreCase(request.getOutputFormat()))
            return false;

        return true;
    }
    
    /**
     * Returns the matched service (case insensitive)
     * @return
     */
    public String getService() {
        return service;
    }

    /**
     * Returns the matched method (case insensitive)
     * @return
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the matched output format (case insensitive)
     * @return
     */
    public String getOutputFormat() {
        return outputFormat;
    }

    @Override
    public String toString() {
        return "BasicOWSController(" + service + "," + method + "," + outputFormat + ","
                + queueSize + ")";
    }

}
