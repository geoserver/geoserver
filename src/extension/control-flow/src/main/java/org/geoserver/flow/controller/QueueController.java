/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.controller;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.flow.FlowController;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

/**
 * Base class for flow controllers using a queue
 *
 * @author Juan Marin, OpenGeo
 */
public abstract class QueueController implements FlowController {
    private static final Logger LOGGER = Logging.getLogger(QueueController.class);

    /** The size of each queue */
    int queueMaxSize;

    /** The per request queue collection */
    Map<String, TimedBlockingQueue> queues = new ConcurrentHashMap<>();

    /** Last time we've performed a queue cleanup */
    long lastCleanup = System.currentTimeMillis();

    /** Number of queues at which we start looking for purging stale ones */
    int maxQueues = 100;

    /** Time it takes for an inactive queue to be considered stale */
    int maxAge = 10000;

    @Override
    public boolean requestIncoming(Request request, long timeout) {
        return false;
    }

    @Override
    public int getPriority() {
        return queueMaxSize;
    }

    protected void cleanUpQueues(long now) {
        // cleanup stale queues if necessary
        int queuesSize = queues.size();
        if ((queuesSize > maxQueues && (now - lastCleanup) > (maxAge / 10))
                || (now - lastCleanup) > maxAge) {
            int cleanupCount = 0;
            synchronized (this) {
                for (String key : queues.keySet()) {
                    TimedBlockingQueue tbq = queues.get(key);
                    if (now - tbq.lastModified > maxAge && tbq.isEmpty()) {
                        queues.remove(key);
                        cleanupCount++;
                    }
                }
                lastCleanup = now;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(
                            getClass().getSimpleName()
                                    + "("
                                    + queueMaxSize
                                    + ") purged "
                                    + cleanupCount
                                    + " stale queues out of "
                                    + queuesSize);
                }
            }
        }
    }

    @SuppressWarnings("serial")
    protected static class TimedBlockingQueue extends ArrayBlockingQueue<Request> {
        long lastModified;

        public TimedBlockingQueue(int capacity, boolean fair) {
            super(capacity, fair);
        }

        @Override
        public void put(Request o) throws InterruptedException {
            super.put(o);
            lastModified = System.currentTimeMillis();
        }

        @Override
        public boolean remove(Object o) {
            lastModified = System.currentTimeMillis();
            return super.remove(o);
        }
    }
}
