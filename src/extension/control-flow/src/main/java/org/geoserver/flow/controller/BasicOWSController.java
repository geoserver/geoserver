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

    public BasicOWSController(String service, int controllerPriority, ThreadBlocker blocker) {
        this(service, null, null, controllerPriority, blocker);
    }

    public BasicOWSController(
            String service, String method, int controllerPriority, ThreadBlocker blocker) {
        this(service, method, null, controllerPriority, blocker);
    }

    public BasicOWSController(
            String service,
            String method,
            String outputFormat,
            int controllerPriority,
            ThreadBlocker blocker) {
        super(new OWSRequestMatcher(service, method, outputFormat), controllerPriority, blocker);
    }

    @Override
    public String toString() {
        return "BasicOWSController(" + matcher + "," + blocker + ")";
    }
}
