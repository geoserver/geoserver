/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import org.geoserver.ows.Request;
import org.junit.After;
import org.junit.Test;

public class RequestSlotsTest {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @After
    public void shutdown() {
        executor.shutdownNow();
    }

    @Test
    public void testAcquire() throws Exception {
        RequestSlots slots = new RequestSlots(2);
        assertEquals(0, slots.size());

        assertTrue(slots.offer(new Request(), 0, MILLISECONDS));
        slots.put(new Request());
        assertEquals(2, slots.size());

        assertFalse(slots.offer(new Request(), 10, MILLISECONDS));
        assertEquals(2, slots.size());
    }

    /** A waiting request gets the slot freed by remove */
    @Test
    public void testWaitAndAcquire() throws Exception {
        RequestSlots slots = new RequestSlots(1);
        Request first = new Request();
        slots.put(first);

        Request second = new Request();
        Future<?> waiting = putAndWait(slots, second);
        assertFalse(waiting.isDone());
        assertEquals(1, slots.size());

        assertTrue(slots.remove(first));
        waiting.get();
        assertEquals(1, slots.size());

        assertTrue(slots.remove(second));
        assertEquals(0, slots.size());
    }

    /** Waiting requests get a slot in arrival order */
    @Test
    public void testArrivalOrder() throws Exception {
        RequestSlots slots = new RequestSlots(1);
        Request running = new Request();
        slots.put(running);
        Request first = new Request();
        Future<?> firstWaiting = putAndWait(slots, first);
        Future<?> secondWaiting = putAndWait(slots, new Request());

        slots.remove(running);
        firstWaiting.get();
        await().during(Duration.ofMillis(100)).until(() -> !secondWaiting.isDone());

        slots.remove(first);
        secondWaiting.get();
    }

    /** A request that timed out holds no slot, and removing it frees nothing */
    @Test
    public void testTimeout() throws Exception {
        RequestSlots slots = new RequestSlots(1);
        Request running = new Request();
        slots.put(running);
        Request timedOut = new Request();

        assertFalse(slots.offer(timedOut, 10, MILLISECONDS));
        assertEquals(1, slots.size());

        // the timed out request was never running, so it frees nothing
        assertFalse(slots.remove(timedOut));
        assertEquals(1, slots.size());
        assertFalse(slots.offer(new Request(), 10, MILLISECONDS));
    }

    /** Removing the same request twice frees one slot only */
    @Test
    public void testRemoveTwice() throws Exception {
        RequestSlots slots = new RequestSlots(1);
        Request request = new Request();
        slots.put(request);

        assertTrue(slots.remove(request));
        assertFalse(slots.remove(request));
        assertEquals(0, slots.size());

        assertTrue(slots.offer(new Request(), 0, MILLISECONDS));
        assertFalse(slots.offer(new Request(), 10, MILLISECONDS));
    }

    /** Calls put on another thread and returns once that thread is parked waiting for a slot */
    private Future<?> putAndWait(RequestSlots slots, Request request) {
        AtomicReference<Thread> thread = new AtomicReference<>();
        Future<?> future = executor.submit(() -> {
            thread.set(Thread.currentThread());
            slots.put(request);
            return null;
        });
        await().until(() -> thread.get() != null && thread.get().getState() == Thread.State.WAITING);
        return future;
    }
}
