/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.geoserver.ows.Request;

/**
 * Simple blocking queue based blocker, a request gets blocked if there are already <code>queueSize
 * </code> requests running
 */
public class SimpleThreadBlocker implements ThreadBlocker {

    /**
     * This queue contains the requests that are running. The ones waiting are not "visible", are
     * all blocked on {@link BlockingQueue#offer(Object, long, TimeUnit)}} or {@link
     * BlockingQueue#put(Object)}
     */
    BlockingQueue<Request> queue;

    public SimpleThreadBlocker(int queueSize) {
        queue = new ArrayBlockingQueue<Request>(queueSize, true);
    }

    public void requestComplete(Request request) {
        // only removes requests that actually locked on the queue, when
        // a timeout happens some flow controllers won't have
        // requestIncoming called, but will have requestComplete called anyways
        queue.remove(request);
    }

    @Override
    public int getRunningRequestsCount() {
        return queue.size();
    }

    public boolean requestIncoming(Request request, long timeout) throws InterruptedException {
        if (timeout > 0) {
            return queue.offer(request, timeout, TimeUnit.MILLISECONDS);
        } else {
            queue.put(request);
            return true;
        }
    }
}
