/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import com.google.common.base.Predicate;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
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

    BlockingQueue<Request> queue;

    int queueSize;

    public SingleQueueFlowController(int queueSize, Predicate<Request> matcher) {
        this.queueSize = queueSize;
        this.matcher = matcher;
        queue = new ArrayBlockingQueue<Request>(queueSize, true);
    }

    public int getPriority() {
        return queueSize;
    }

    public void requestComplete(Request request) {
        if (matcher.apply(request)) {
            queue.remove(request);
        }
    }

    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        if (matcher.apply(request)) {
            try {
                if (timeout > 0) {
                    retval = queue.offer(request, timeout, TimeUnit.MILLISECONDS);
                } else {
                    queue.put(request);
                }
            } catch (InterruptedException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Unexpected interruption while blocking on the request queue");
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(this + " queue size " + queue.size());
            }
        }
        return retval;
    }

    public Predicate<Request> getMatcher() {
        return matcher;
    }

    /**
     * Returns the current queue size
     *
     * @return
     */
    public int getRequestsInQueue() {
        return queue.size();
    }
}
