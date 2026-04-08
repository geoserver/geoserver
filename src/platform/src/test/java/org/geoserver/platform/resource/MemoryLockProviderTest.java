/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Test;

/**
 * Tests the memory lock, including its ability to be reentrant. The keys for meta-tile and tile caching are different,
 * so we don't really need it to handle nested locks on the same key, but we want to make sure it doesn't break if we
 * ever had two threads in the same JVM using resources in a nested way (just for extra safety, it should not happen).
 */
public class MemoryLockProviderTest {

    /** Long timeout held to avoid CI jitter; tests should pass much faster than this */
    private static final int LONG_TIMEOUT_SECONDS = 5;

    /** A small window for "should not happen yet" checks */
    private static final int SHORT_WINDOW_MILLIS = 200;

    /** Do not leak interrupt status to other tests */
    @After
    public void clearInterruptFlag() {
        Thread.interrupted();
    }

    private static void awaitForLockCleanup(MemoryLockProvider provider, String key) {
        // Be tolerant in CI; await returns immediately once condition is true
        await().atMost(2, SECONDS).until(() -> !provider.lockAndCounters.containsKey(key));
    }

    private static void awaitLatch(CountDownLatch latch, String message) throws InterruptedException {
        assertTrue(message, latch.await(LONG_TIMEOUT_SECONDS, SECONDS));
    }

    private static void assertLatchNotReleasedYet(CountDownLatch latch, String message) throws InterruptedException {
        assertFalse(message, latch.await(SHORT_WINDOW_MILLIS, MILLISECONDS));
    }

    private static void awaitFuture(Future<?> future) throws Exception {
        future.get(LONG_TIMEOUT_SECONDS, SECONDS);
    }

    private static void shutdownNow(ExecutorService exec) {
        exec.shutdownNow();
    }

    /** Runs {@code body} with a fixed thread pool and guarantees shutdown + exception propagation. */
    private static void withExecutor(int threads, ThrowingConsumer<ExecutorService> body) throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        try {
            body.accept(exec);
        } finally {
            shutdownNow(exec);
        }
    }

    /**
     * Submits a task that acquires {@code key}, signals {@code hasLock}, then waits for {@code allowRelease} before
     * releasing.
     */
    private static Future<?> submitLockHolder(
            ExecutorService exec,
            MemoryLockProvider provider,
            String key,
            CountDownLatch hasLock,
            CountDownLatch allowRelease,
            String threadNameForMessages) {

        return exec.submit(() -> {
            Resource.Lock lock = provider.acquire(key);
            try {
                hasLock.countDown();
                assertTrue(
                        threadNameForMessages + ": release signal not received",
                        allowRelease.await(LONG_TIMEOUT_SECONDS, SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(threadNameForMessages + " interrupted");
            } finally {
                lock.release();
            }
        });
    }

    /** Submits a task that waits for {@code startSignal}, then acquires {@code key} and signals {@code entered}. */
    private static Future<?> submitLockAcquirer(
            ExecutorService exec,
            MemoryLockProvider provider,
            String key,
            CountDownLatch startSignal,
            CountDownLatch entered,
            String threadNameForMessages) {

        return exec.submit(() -> {
            try {
                awaitLatch(startSignal, threadNameForMessages + ": start signal not received");
                Resource.Lock lock = provider.acquire(key);
                try {
                    entered.countDown();
                } finally {
                    lock.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail(threadNameForMessages + " interrupted");
            }
        });
    }

    @FunctionalInterface
    private interface ThrowingConsumer<T> {
        void accept(T value) throws Exception;
    }

    @Test
    public void testAcquireReleaseRemovesEntry() {
        MemoryLockProvider provider = new MemoryLockProvider();
        assertTrue(provider.lockAndCounters.isEmpty());

        Resource.Lock lock = provider.acquire("k");
        assertNotNull(lock);
        assertTrue(provider.lockAndCounters.containsKey("k"));

        lock.release();
        awaitForLockCleanup(provider, "k");
    }

    @Test
    public void testReleaseIsIdempotent() {
        MemoryLockProvider provider = new MemoryLockProvider();

        Resource.Lock lock = provider.acquire("k");
        lock.release();
        lock.release(); // must be a no-op

        awaitForLockCleanup(provider, "k");
    }

    @Test
    public void testNestedLocksSameThread() {
        MemoryLockProvider provider = new MemoryLockProvider();

        // Nested (re-entrant) acquires on the same key
        Resource.Lock l1 = provider.acquire("k");
        Resource.Lock l2 = provider.acquire("k");
        Resource.Lock l3 = provider.acquire("k");

        assertTrue("Entry should exist while nested locks are held", provider.lockAndCounters.containsKey("k"));

        // Release in a different order than acquisition
        l2.release();
        assertTrue("Entry should still exist after partial release", provider.lockAndCounters.containsKey("k"));

        l1.release();
        assertTrue("Entry should still exist while one nested lock is held", provider.lockAndCounters.containsKey("k"));

        l3.release();
        awaitForLockCleanup(provider, "k");
    }

    @Test
    public void testMutualExclusionSameKey() throws Exception {
        MemoryLockProvider provider = new MemoryLockProvider();

        withExecutor(2, exec -> {
            CountDownLatch t1HasLock = new CountDownLatch(1);
            CountDownLatch allowT1ToRelease = new CountDownLatch(1);
            CountDownLatch t2EnteredCriticalSection = new CountDownLatch(1);

            Future<?> t1 = submitLockHolder(exec, provider, "k", t1HasLock, allowT1ToRelease, "t1");
            Future<?> t2 = submitLockAcquirer(exec, provider, "k", t1HasLock, t2EnteredCriticalSection, "t2");

            // While t1 holds the lock, t2 must NOT enter.
            assertLatchNotReleasedYet(
                    t2EnteredCriticalSection, "t2 should not enter critical section while t1 holds the lock");

            // Once t1 releases, t2 must enter.
            allowT1ToRelease.countDown();
            awaitLatch(t2EnteredCriticalSection, "t2 should enter critical section after t1 releases");

            // Propagate worker exceptions
            awaitFuture(t1);
            awaitFuture(t2);
        });

        awaitForLockCleanup(provider, "k");
    }

    @Test
    public void testDifferentKeysDoNotBlockEachOther() throws Exception {
        MemoryLockProvider provider = new MemoryLockProvider();

        withExecutor(2, exec -> {
            CountDownLatch t1HasA = new CountDownLatch(1);
            CountDownLatch allowT1ToReleaseA = new CountDownLatch(1);
            CountDownLatch t2AcquiredB = new CountDownLatch(1);

            Future<?> t1 = submitLockHolder(exec, provider, "A", t1HasA, allowT1ToReleaseA, "t1");
            Future<?> t2 = submitLockAcquirer(exec, provider, "B", t1HasA, t2AcquiredB, "t2");

            // B should be acquirable even while A is held.
            awaitLatch(t2AcquiredB, "B should be acquirable even while A is held");

            // Cleanup
            allowT1ToReleaseA.countDown();
            awaitFuture(t1);
            awaitFuture(t2);
        });

        await().atMost(2, SECONDS).until(() -> provider.lockAndCounters.isEmpty());
    }

    @Test
    public void testInterruptedWhileAcquiringThrowsAndPreservesInterruptFlag() throws Exception {
        MemoryLockProvider provider = new MemoryLockProvider();

        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            CountDownLatch holderHasLock = new CountDownLatch(1);
            CountDownLatch allowHolderToRelease = new CountDownLatch(1);

            Future<?> holder = submitLockHolder(exec, provider, "k", holderHasLock, allowHolderToRelease, "holder");

            awaitLatch(holderHasLock, "holder did not acquire lock in time");

            // Interrupt *this* thread before trying to acquire -> tryLock should throw InterruptedException,
            // MemoryLockProvider should re-interrupt the thread and wrap into RuntimeException
            Thread.currentThread().interrupt();
            try {
                provider.acquire("k");
                fail("Expected RuntimeException due to interruption");
            } catch (RuntimeException expected) {
                // ok
            }

            assertTrue(
                    "Interrupt flag should be preserved", Thread.currentThread().isInterrupted());

            // IMPORTANT: clear interrupt before cleanup that blocks
            Thread.interrupted();

            allowHolderToRelease.countDown();
            awaitFuture(holder);
        } finally {
            shutdownNow(exec);
        }
    }
}
