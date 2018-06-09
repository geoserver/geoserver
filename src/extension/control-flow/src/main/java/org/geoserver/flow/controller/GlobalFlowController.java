/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

/**
 * A flow controller matching all requests, can be used for globally controlling the number of
 * incoming requests
 *
 * @author Andrea Aime - OpenGeo
 */
public class GlobalFlowController extends SingleQueueFlowController {

    public GlobalFlowController(int controllerPriority, ThreadBlocker blocker) {
        super(new OWSRequestMatcher(), controllerPriority, blocker);
    }

    @Override
    public String toString() {
        return "GlobalFlowController(" + blocker + ")";
    }
}
