/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.controller;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

/**
 * A flow controller that throttles concurrent requests made from the same ip (any ip)
 *
 * @author Juan Marin, OpenGeo
 */
public class IpFlowController extends QueueController {

    /**
     * Thread local holding the current request queue id TODO: consider having a user map in {@link
     * Request} instead
     */
    static ThreadLocal<String> QUEUE_ID = new ThreadLocal<String>();

    /**
     * A flow controller that throttles concurrent requests made from the same ip (any ip)
     *
     * @author Juan Marin, OpenGeo
     */
    static final Logger LOGGER = Logging.getLogger(IpFlowController.class);

    public IpFlowController(int queueSize) {
        this.queueSize = queueSize;
    }

    @Override
    public void requestComplete(Request request) {
        String queueId = QUEUE_ID.get();
        QUEUE_ID.remove();
        if (queueId != null) {
            BlockingQueue<Request> queue = queues.get(queueId);
            if (queue != null) queue.remove(request);
        }
    }

    @Override
    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        // check if this client already made other connections
        final String incomingIp;
        {
            String ip = getRemoteAddr(request.getHttpRequest());
            if (null == ip || "".equals(ip)) {
                // may this happen? hope not, but if someone is trying to trick us lets not let him
                // and pool it on the "empty IP" queue
                incomingIp = "";
            } else {
                incomingIp = ip;
            }
        }

        // see if we have that queue already
        TimedBlockingQueue queue = queues.get(incomingIp);

        // generate a unique queue id for this client if none was found
        if (queue == null) {
            // beware of multiple concurrent requests...
            synchronized (this) {
                queue = queues.get(incomingIp);
                if (queue == null) {
                    queue = new TimedBlockingQueue(queueSize, true);
                    queues.put(incomingIp, queue);
                }
            }
        }
        QUEUE_ID.set(incomingIp);

        // queue token handling
        try {
            if (timeout > 0) {
                retval = queue.offer(request, timeout, TimeUnit.MILLISECONDS);
            } else {
                queue.put(request);
            }
        } catch (InterruptedException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Unexpected interruption while " + "blocking on the request queue");
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "IpFlowController("
                            + queueSize
                            + ","
                            + incomingIp
                            + ") queue size "
                            + queue.size());
            LOGGER.fine(
                    "IpFlowController("
                            + queueSize
                            + ","
                            + incomingIp
                            + ") total queues "
                            + queues.size());
        }
        return retval;
    }

    static String getRemoteAddr(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            if (-1 == forwardedFor.indexOf(',')) {
                return forwardedFor;
            }
            String[] ips = forwardedFor.split(", ");
            return ips[0];
        } else {
            return req.getRemoteAddr();
        }
    }
}
