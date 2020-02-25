/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import com.google.common.base.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.flow.ControlFlowCallback;
import org.geoserver.flow.FlowController;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

/**
 * Base class for flow controllers using a single queue
 *
 * @author Andrea Aime - OpenGeo
 */
public class SingleQueueFlowController implements FlowController {
    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    Predicate<Request> matcher;

    ThreadBlocker blocker;

    int controllerPriority;

    public SingleQueueFlowController(
            Predicate<Request> matcher, int controllerPriority, ThreadBlocker blocker) {
        this.controllerPriority = controllerPriority;
        this.matcher = matcher;
        this.blocker = blocker;
    }

    public int getPriority() {
        return controllerPriority;
    }

    public void requestComplete(Request request) {
        if (matcher.apply(request)) {
            blocker.requestComplete(request);
        }
    }

    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        if (matcher.apply(request)) {
            try {
                retval = blocker.requestIncoming(request, timeout);
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Unexpected interruption while waiting for execution");
            }
        }
        return retval;
    }

    public Predicate<Request> getMatcher() {
        return matcher;
    }

    /** Returns the current queue size (used for testing only) */
    public int getRequestsInQueue() {
        return blocker.getRunningRequestsCount();
    }

    /**
     * Returns the thread blocking mechanisms for this queue
     *
     * @return a {@link ThreadBlocker} instance
     */
    public ThreadBlocker getBlocker() {
        return blocker;
    }
}
