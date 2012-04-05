/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.flow.controller;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import org.geoserver.flow.FlowController;
import org.geoserver.ows.Request;

/**
 * Base class for flow controllers using a queue
 * 
 * @author Juan Marin, OpenGeo
 * 
 */

public abstract class QueueController implements FlowController {

    /**
     * Thread local holding the current request queue id TODO: consider having a user map in {@link Request} instead
     */
    static ThreadLocal<String> QUEUE_ID = new ThreadLocal<String>();

    /**
     * The size of each queue
     */
    int queueSize;

    /**
     * The per request queue collection
     */
    Map<String, TimedBlockingQueue> queues = new ConcurrentHashMap<String, TimedBlockingQueue>();

    @Override
    public boolean requestIncoming(Request request, long timeout) {
        return false;
    }

    @Override
    public void requestComplete(Request request) {
        String queueId = QUEUE_ID.get();
        QUEUE_ID.remove();
        BlockingQueue<Request> queue = queues.get(queueId);
        if (queue != null)
            queue.remove(request);
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
