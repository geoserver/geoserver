/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow;

import org.geoserver.ows.Request;

/**
 * A class that can control the flow of incoming OWS requests. Implementation can use blocking
 * queues to make sure no more than a certain number of requests can run in parallel, or delay the
 * current request a certain amount of time, and so on
 *
 * @author Andrea Aime - OpenGeo
 */
public interface FlowController {

    /**
     * Called when a new request is passed into the Dispatcher
     *
     * @param request new request
     * @param timeout maximum time the request can be blocked
     * @return True if the request was processed successfully, false if the request timed out during
     *     the wait
     */
    boolean requestIncoming(Request request, long timeout);

    /**
     * Called when the request is done its processing (will be called both for executing and timeout
     * out requests to ensure eventually required clean ups)
     *
     * @param request the request
     */
    void requestComplete(Request request);

    /**
     * Returns the flow controller "priority", determines the order in which the controllers are
     * being called, from lower to higher (not to be confused with the request priority). For
     * controllers that limit the number of incoming requests by using a blocking queue it is
     * advised to use the queue size itself as the controller priority.
     */
    int getPriority();
}
