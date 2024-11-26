/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.controller;

import static org.geoserver.flow.ControlFlowCallback.X_CONCURRENT_LIMIT;
import static org.geoserver.flow.ControlFlowCallback.X_CONCURRENT_REQUESTS;

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

    /** Thread local holding the current request queue id */
    static ThreadLocal<String> QUEUE_ID = new ThreadLocal<>();

    /**
     * A flow controller that throttles concurrent requests made from the same ip (any ip)
     *
     * @author Juan Marin, OpenGeo
     */
    static final Logger LOGGER = Logging.getLogger(IpFlowController.class);

    public IpFlowController(int queueSize) {
        this.queueMaxSize = queueSize;
    }

    @Override
    public void requestComplete(Request request) {
        String queueId = QUEUE_ID.get();
        QUEUE_ID.remove();
        if (queueId != null) {
            TimedBlockingQueue queue = queues.get(queueId);
            if (queue != null) queue.remove(request);
        }
    }

    @Override
    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        long now = System.currentTimeMillis();

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
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(
                                "IpFlowController("
                                        + queueMaxSize
                                        + "),"
                                        + incomingIp
                                        + ", creating new queue");
                    }
                    queue = new TimedBlockingQueue(queueMaxSize, true);
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

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "IpFlowController("
                                + queueMaxSize
                                + ") "
                                + incomingIp
                                + ", concurrent requests: "
                                + queue.size());
            }
            request.getHttpResponse()
                    .addHeader(X_CONCURRENT_LIMIT + "-ip", String.valueOf(queueMaxSize));
            request.getHttpResponse()
                    .addHeader(X_CONCURRENT_REQUESTS + "-ip", String.valueOf(queue.size()));
        } catch (InterruptedException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Unexpected interruption while " + "blocking on the request queue");
        }
        // cleanup stale queues if necessary
        cleanUpQueues(now);

        // logs about queue size
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(
                    "IpFlowController("
                            + queueMaxSize
                            + ","
                            + incomingIp
                            + ") queue size "
                            + queue.size());
        }

        return retval;
    }

    static String getRemoteAddr(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        String ip;
        if (forwardedFor != null) {
            if (-1 == forwardedFor.indexOf(',')) {
                return forwardedFor;
            }
            String[] ips = forwardedFor.split(", ");
            ip = ips[0];
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("X-Forwarded-For: " + forwardedFor + " -> " + ip);
            }
        } else {
            ip = req.getRemoteAddr();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("X-Forwarded-For missing, ip from servlet request " + ip);
            }
        }
        return ip;
    }

    @Override
    public String toString() {
        return "IpFlowController(" + queueMaxSize + ")";
    }
}
