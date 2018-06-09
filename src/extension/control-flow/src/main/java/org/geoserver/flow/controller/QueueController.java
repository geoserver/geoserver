/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.controller;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.flow.FlowController;
import org.geoserver.ows.Request;

/**
 * Base class for flow controllers using a queue
 *
 * @author Juan Marin, OpenGeo
 */
public abstract class QueueController implements FlowController {
    /** The size of each queue */
    int queueSize;

    /** The per request queue collection */
    Map<String, TimedBlockingQueue> queues = new ConcurrentHashMap<String, TimedBlockingQueue>();

    @Override
    public boolean requestIncoming(Request request, long timeout) {
        return false;
    }

    @Override
    public int getPriority() {
        return queueSize;
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
