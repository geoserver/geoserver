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
import org.geoserver.flow.ControlFlowCallback;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

/**
 * A flow controller setting a cookie on HTTP request and making sure the same user cannot do more
 * than X requests in parallel. Warning: if a client does not support cookies this class cannot work
 * properly and will start accumulating queues with just one item inside. As a workaround when too
 * many queues are accumulated a scan starts that purges all queues that are empty and have not been
 * touched within a given amount of time: the idea is that a past that time we're assuming the
 * client is no more working actively against the server and the queue can thus be removed.
 *
 * @author Andrea Aime - OpenGeo
 * @author Juan Marin, OpenGeo
 */
public class UserConcurrentFlowController extends QueueController {
    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    /**
     * Thread local holding the current request queue id TODO: consider having a user map in {@link
     * Request} instead
     */
    static ThreadLocal<String> QUEUE_ID = new ThreadLocal<String>();

    CookieKeyGenerator keyGenerator = new CookieKeyGenerator();

    /** Last time we've performed a queue cleanup */
    long lastCleanup = System.currentTimeMillis();

    /** Number of queues at which we start looking for purging stale ones */
    int maxQueues = 100;

    /** Time it takes for an inactive queue to be considered stale */
    int maxAge = 10000;

    /**
     * Builds a UserFlowController that will trigger stale queue expiration once 100 queues have
     * been accumulated and
     *
     * @param queueSize the maximum amount of per user concurrent requests
     */
    public UserConcurrentFlowController(int queueSize) {
        this(queueSize, 100, 10000);
    }

    /**
     * Builds a new {@link UserConcurrentFlowController}
     *
     * @param queueSize the maximum amount of per user concurrent requests
     * @param maxQueues the number of accumulated user queues that will trigger a queue cleanup
     * @param maxAge the max quiet time for an empty queue to be considered stale and removed
     */
    public UserConcurrentFlowController(int queueSize, int maxQueues, int maxAge) {
        this.queueSize = queueSize;
        this.maxQueues = maxQueues;
        this.maxAge = maxAge;
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

    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        long now = System.currentTimeMillis();

        String queueId = keyGenerator.getUserKey(request);
        QUEUE_ID.set(queueId);

        // see if we have that queue already, otherwise generate it
        TimedBlockingQueue queue = null;
        queue = queues.get(queueId);
        if (queue == null) {
            queue = new TimedBlockingQueue(queueSize, true);
            queues.put(queueId, queue);
        }

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
                    "UserFlowController("
                            + queueSize
                            + ","
                            + queueId
                            + ") queue size "
                            + queue.size());
            LOGGER.fine(
                    "UserFlowController("
                            + queueSize
                            + ","
                            + queueId
                            + ") total queues "
                            + queues.size());
        }

        // cleanup stale queues if necessary
        if ((queues.size() > maxQueues && (now - lastCleanup) > (maxAge / 10))
                || (now - lastCleanup) > maxAge) {
            int cleanupCount = 0;
            synchronized (this) {
                for (String key : queues.keySet()) {
                    TimedBlockingQueue tbq = queues.get(key);
                    if (now - tbq.lastModified > maxAge && tbq.size() == 0) {
                        queues.remove(key);
                        cleanupCount++;
                    }
                }
                lastCleanup = now;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            "UserFlowController("
                                    + queueSize
                                    + ") purged "
                                    + cleanupCount
                                    + " stale queues");
                }
            }
        }

        return retval;
    }
}
