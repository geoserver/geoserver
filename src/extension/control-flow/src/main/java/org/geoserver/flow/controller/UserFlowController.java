/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import java.rmi.server.UID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;

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
public class UserFlowController extends QueueController {
    static final Logger LOGGER = Logging.getLogger(ControlFlowCallback.class);

    static String COOKIE_NAME = "GS_FLOW_CONTROL";

    static String COOKIE_PREFIX = "GS_CFLOW_";
    
    /**
     * Thread local holding the current request queue id TODO: consider having a user map in {@link Request} instead
     */
    static ThreadLocal<String> QUEUE_ID = new ThreadLocal<String>();
    
    /**
     * Last time we've performed a queue cleanup
     */
    long lastCleanup = System.currentTimeMillis();

    /**
     * Number of queues at which we start looking for purging stale ones
     */
    int maxQueues = 100;

    /**
     * Time it takes for an inactive queue to be considered stale
     */
    int maxAge = 10000;

    

    /**
     * Builds a UserFlowController that will trigger stale queue expiration once 100 queues have
     * been accumulated and
     * 
     * @param queueSize
     *            the maximum amount of per user concurrent requests
     */
    public UserFlowController(int queueSize) {
        this(queueSize, 100, 10000);
    }

    /**
     * Builds a new {@link UserFlowController}
     * 
     * @param queueSize
     *            the maximum amount of per user concurrent requests
     * @param maxQueues
     *            the number of accumulated user queues that will trigger a queue cleanup
     * @param maxAge
     *            the max quiet time for an empty queue to be considered stale and removed
     */
    public UserFlowController(int queueSize, int maxQueues, int maxAge) {
        this.queueSize = queueSize;
        this.maxQueues = maxQueues;
        this.maxAge = maxAge;
    }
    
    @Override
    public void requestComplete(Request request) {
        String queueId = QUEUE_ID.get();
        QUEUE_ID.remove();
        if(queueId != null) {
            BlockingQueue<Request> queue = queues.get(queueId);
            if (queue != null)
                queue.remove(request);
        }
    }

    public boolean requestIncoming(Request request, long timeout) {
        boolean retval = true;
        long now = System.currentTimeMillis();

        // check if this client already made other connections
        Cookie idCookie = null;
        Cookie[] cookies = request.getHttpRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(COOKIE_NAME)) {
                    idCookie = cookie;
                    break;
                }
            }
        }

        // see if we have that queue already
        TimedBlockingQueue queue = null;
        if (idCookie != null) {
            queue = queues.get(idCookie.getValue());
        }
        // generate a unique queue id for this client if none was found
        if(queue == null) {
            idCookie = new Cookie(COOKIE_NAME, COOKIE_PREFIX + new UID().toString());
            queue = new TimedBlockingQueue(queueSize, true);
            queues.put(idCookie.getValue(), queue);
        } 
        QUEUE_ID.set(idCookie.getValue());
        request.getHttpResponse().addCookie(idCookie);

        // queue token handling
        try {
            if(timeout > 0) {
                retval = queue.offer(request, timeout, TimeUnit.MILLISECONDS);
            } else {
                queue.put(request);
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Unexpected interruption while "
                    + "blocking on the request queue");
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("UserFlowController(" + queueSize + "," + idCookie.getValue()
                    + ") queue size " + queue.size());
            LOGGER.fine("UserFlowController(" + queueSize + "," + idCookie.getValue()
                    + ") total queues " + queues.size());
        }

        // cleanup stale queues if necessary
        if ((queues.size() > maxQueues && (now - lastCleanup) > (maxAge / 10))
                || (now - lastCleanup) > maxAge) {
            int cleanupCount = 0;
            synchronized (queues) {
                for (String key : queues.keySet()) {
                    TimedBlockingQueue tbq = queues.get(key);
                    if (now - tbq.lastModified > maxAge && tbq.size() == 0) {
                        queues.remove(key);
                        cleanupCount++;
                    }
                }
                lastCleanup = now;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("UserFlowController(" + queueSize + ") purged " + cleanupCount
                            + " stale queues");
                }
            }
        }
        
        return retval;
    }

    
}
