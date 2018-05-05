/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Blocking queue based blocker, a request gets blocked if there are already <code>queueSize</code> requests
 * running. Unlike {@link SimpleThreadBlocker} here threads that got blocked due to full queue will
 * be awaken in priority order, highest to lowest
 */
public class PriorityThreadBlocker implements ThreadBlocker {

    static final Logger LOGGER = Logging.getLogger(PriorityThreadBlocker.class);

    private final PriorityProvider priorityProvider;
    private final int maxRunningRequests;
    // unlike the SimpleThreadBlock this does not contain the requests that were freed to go onto the next
    // controller or execution, but the ones blocked waiting
    private final PriorityQueue<WaitToken> queue = new PriorityQueue<>();
    // This holds the requests actually running on this blocker. Flow controllers 
    // might not all be called if one fails, but all get a "requestComplete" for cleanup,
    // so need to know if this blocker was called before, or not
    private final Set<Request> runningQueue = new HashSet<>();

    public PriorityThreadBlocker(int queueSize, PriorityProvider priorityProvider) {
        this.maxRunningRequests = queueSize;
        this.priorityProvider = priorityProvider;
    }

    @Override
    public int getRunningRequestsCount() {
        return queue.size();
    }

    public boolean requestIncoming(Request request, long timeout) throws InterruptedException {
        WaitToken token = null;

        boolean result = false;

        // protect shared data structures from MT access
        synchronized (this) {
            if (runningQueue.size() < maxRunningRequests) {
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.log(Level.FINER, "Running requests at " + runningQueue.size() + ", no block");
                }
                result = true;
            } else {
                int priority = priorityProvider.getPriority(request);
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.log(Level.FINER, "Running requests at " + runningQueue.size() + ", Queuing request with " +
                            "priority " + priority);
                }
                token = new WaitToken(priority);
                queue.add(token);
            }
        }

        // if this request entered the queue, wait for the latch to be released
        if (token != null) {
            if (timeout > 0) {
                result = token.latch.await(timeout, TimeUnit.MILLISECONDS);
                synchronized (this) {
                    // if timeout out, just remove from the queue
                    if (!result) {
                        if (LOGGER.isLoggable(Level.FINER)) {
                            LOGGER.log(Level.FINER, "Request with priority " + token.priority + " timed out, removing" +
                                    " from" +
                                    " queue");
                        }
                        boolean removed = queue.remove(token);
                        if (!removed) {
                            if (LOGGER.isLoggable(Level.FINER)) {
                                LOGGER.log(Level.FINER, "Request was not found in queue, releasing next");
                            }
                            // has already been removed by releaseNext, release the next one then
                            if (runningQueue.size() < maxRunningRequests) {
                                releaseNext();
                            }
                        }
                    }
                }
            } else {
                token.latch.await();
                result = true;
            }
        }

        // the code will call requestComplete also in case of timeout, need to keep the balance
        synchronized (this) {
            runningQueue.add(request);
        }

        return result;
    }

    public void requestComplete(Request request) {
        // protect shared data structures from MT
        synchronized (this) {
            runningQueue.remove(request);
            if (runningQueue.size() < maxRunningRequests) {
                releaseNext();
            }
        }

    }

    private void releaseNext() {
        // this needs to be called within a synchronized section
        assert Thread.holdsLock(this);

        WaitToken token;
        token = queue.poll();
        if (token != null) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER, "Releasing request with priority " + token.priority);
            }

            token.latch.countDown();

        }
    }

    /**
     * Returns the priority provider, issuing a priority for each request to be put in queue
     *
     * @return
     */
    public PriorityProvider getPriorityProvider() {
        return priorityProvider;
    }

    /**
     * Simple token for the priority queue, holds the priority, sorts on it higher to lower, and
     * holds the latch blocking the thread
     */
    private static class WaitToken implements Comparable<WaitToken> {
        CountDownLatch latch = new CountDownLatch(1);
        long created = System.currentTimeMillis();
        int priority;

        public WaitToken(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(WaitToken o) {
            // to have the highest priority first (smallest) in the queue
            int diff = o.priority - this.priority;
            if (diff != 0) {
                return diff;
            } else {
                // in case of same priority, first come first served
                return Long.signum(this.created - o.created);
            }
        }
    }

}
