/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

/**
 * A flow controller that can categorize requests by service, method and output format
 *
 * @author Andrea Aime - OpenGeo
 */
public class BasicOWSController extends SingleQueueFlowController {
    public BasicOWSController(String service, int queueSize) {
        this(service, null, null, queueSize);
    }

    public BasicOWSController(String service, String method, int queueSize) {
        this(service, method, null, queueSize);
    }

    public BasicOWSController(String service, String method, String outputFormat, int queueSize) {
        super(queueSize, new OWSRequestMatcher(service, method, outputFormat));
    }

    @Override
    public String toString() {
        return "BasicOWSController(" + matcher + "," + queueSize + ")";
    }
}
