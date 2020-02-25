/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import org.geoserver.ows.Request;

/** Mechanism to block a thread on flow controllers. */
public interface ThreadBlocker {

    /**
     * Called by the flow controller to check if there is a need to block
     *
     * @param request new request
     * @param timeout maximum time the request can be blocked
     * @return True if the request was processed successfully, false if the request timed out during
     *     the wait
     */
    boolean requestIncoming(Request request, long timeout) throws InterruptedException;

    /**
     * Called by the flow controller when the request is done processingt
     *
     * @param request the request
     */
    void requestComplete(Request request);

    /**
     * Returns the number of requests "running" (could be both executing and timed out, anything
     * that went into {@link #requestIncoming(Request, long)} and managed to get out of it, but it's
     * not yet complete)
     */
    int getRunningRequestsCount();
}
