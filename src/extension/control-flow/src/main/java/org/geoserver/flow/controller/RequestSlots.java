/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.geoserver.ows.Request;

/**
 * Bounded set of running requests, designed to replace ArrayBlockingQueue. An ArrayBlockingQueue takes one lock on
 * every add and remove, so all requests wait on each other even when the limit is far away. Here a request that finds a
 * free slot takes no lock.
 */
class RequestSlots {

    private final int size;

    // the concurrency primitive that actually blocks excess requests
    private final Semaphore permits;

    /** Tracks the requests holding a slot */
    private final Set<Request> running = ConcurrentHashMap.newKeySet();

    RequestSlots(int size) {
        this.size = size;
        // fair, so waiters get a slot in arrival order, taking a free permit is still one atomic update
        this.permits = new Semaphore(size, true);
    }

    /** Same contract as {@link BlockingQueue#offer(Object, long, TimeUnit)} */
    boolean offer(Request request, long timeout, TimeUnit unit) throws InterruptedException {
        if (!permits.tryAcquire(timeout, unit)) return false;
        running.add(request);
        return true;
    }

    void put(Request request) throws InterruptedException {
        permits.acquire();
        running.add(request);
    }

    /** Frees the slot if the request holds one, returns false otherwise */
    boolean remove(Request request) {
        if (!running.remove(request)) return false;
        permits.release();
        return true;
    }

    int size() {
        return size - permits.availablePermits();
    }
}
