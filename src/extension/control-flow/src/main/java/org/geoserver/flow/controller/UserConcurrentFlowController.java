/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static org.geoserver.flow.ControlFlowCallback.X_CONCURRENT_LIMIT;
import static org.geoserver.flow.ControlFlowCallback.X_CONCURRENT_REQUESTS;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.flow.ControlFlowCallback;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

/**
 * A flow controller setting a cookie on HTTP request and making sure the same user cannot do more than X requests in
 * parallel. Warning: if a client does not support cookies this class cannot work properly and will start accumulating
 * queues with just one item inside. As a workaround when too many queues are accumulated a scan starts that purges all
 * queues that are empty and have not been touched within a given amount of time: the idea is that a past that time
 * we're assuming the client is no more working actively against the server and the queue can thus be removed.
 *
 * @author Andrea Aime - OpenGeo
 * @author Juan Marin, OpenGeo
 */
public class UserConcurrentFlowController extends QueueController {
    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    /** Thread local holding the current request queue id TODO: consider having a user map in {@link Request} instead */
    static ThreadLocal<String> QUEUE_ID = new ThreadLocal<>();

    CookieKeyGenerator keyGenerator = new CookieKeyGenerator();

    /**
     * Builds a UserFlowController that will trigger stale queue expiration once 100 queues have been accumulated and
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
        this.queueMaxSize = queueSize;
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

    @Override
    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        long now = System.currentTimeMillis();

        String queueId = keyGenerator.getUserKey(request);
        QUEUE_ID.set(queueId);

        // see if we have that queue already, otherwise generate it
        TimedBlockingQueue queue = queues.get(queueId);
        if (queue == null) {
            queue = new TimedBlockingQueue(queueMaxSize, true);
            queues.put(queueId, queue);
        }

        // queue token handling
        try {
            if (timeout > 0) {
                retval = queue.offer(request, timeout, TimeUnit.MILLISECONDS);
            } else {
                queue.put(request);
            }

            request.getHttpResponse().addHeader(X_CONCURRENT_LIMIT + "-user", String.valueOf(queueMaxSize));
            request.getHttpResponse().addHeader(X_CONCURRENT_REQUESTS + "-user", String.valueOf(queue.size()));
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Unexpected interruption while " + "blocking on the request queue");
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("UserFlowController(" + queueMaxSize + "," + queueId + ") queue size " + queue.size());
        }

        // cleanup stale queues if necessary
        cleanUpQueues(now);

        return retval;
    }

    @Override
    public String toString() {
        return "UserConcurrentFlowController{" + "queueMaxSize=" + queueMaxSize + '}';
    }
}
