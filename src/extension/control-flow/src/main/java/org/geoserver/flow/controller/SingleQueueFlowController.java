/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

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
 * 
 */
public abstract class SingleQueueFlowController implements FlowController {
    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    BlockingQueue<Request> queue;

    int queueSize;

    public SingleQueueFlowController(int queueSize) {
        this.queueSize = queueSize;
        queue = new ArrayBlockingQueue<Request>(queueSize, true);
    }

    public int getPriority() {
        return queueSize;
    }

    public void requestComplete(Request request) {
        if (matchesRequest(request)) {
            queue.remove(request);
        }
    }

    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        if (matchesRequest(request)) {
            try {
                if(timeout > 0) {
                    retval = queue.offer(request, timeout, TimeUnit.MILLISECONDS);
                } else {
                    queue.put(request);
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING,
                        "Unexpected interruption while blocking on the request queue");
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(this + " queue size " + queue.size());
            }

        }
        return retval;
    }

    abstract boolean matchesRequest(Request request);

}
