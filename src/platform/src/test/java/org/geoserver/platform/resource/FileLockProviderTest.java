/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests for {@link FileLockProvider}, including reentrant (nested) locking on the same thread. */
public class FileLockProviderTest {

    /** Small timeout for testing failure cases quickly */
    private static final int TEST_TIMEOUT_SECONDS = 1;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private FileLockProvider provider;
    private File root;

    @Before
    public void setUp() throws Exception {
        root = tempFolder.newFolder("lockRoot");
        provider = new FileLockProvider(root, TEST_TIMEOUT_SECONDS);
    }

    @Test
    public void testAcquireAndReleaseDeletesFile() throws Exception {
        String key = "test-key";
        Resource.Lock lock = provider.acquire(key);

        File lockFile = getLockFile(key);
        assertTrue("Lock file should exist on disk", lockFile.exists());

        lock.release();

        // Use awaitility to handle potential OS delay in file deletion
        await().atMost(2, SECONDS).until(() -> !lockFile.exists());
    }

    @Test
    public void testLockTimeoutThrowsException() throws Exception {
        String key = "timeout-key";
        // A second provider instance has its own gate, so contention between the two can only
        // be enforced by the real OS-level file lock, as would happen across two processes
        FileLockProvider provider2 = new FileLockProvider(root, TEST_TIMEOUT_SECONDS);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            CountDownLatch threadAHold = new CountDownLatch(1);
            CountDownLatch releaseThreadA = new CountDownLatch(1);

            executor.submit(() -> {
                Resource.Lock lock = provider.acquire(key);
                threadAHold.countDown();
                try {
                    releaseThreadA.await(5, SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.release();
                }
            });

            assertTrue("Thread A failed to acquire lock", threadAHold.await(2, SECONDS));

            long start = System.currentTimeMillis();
            try {
                // Use the SECOND provider; it will be blocked by the file on disk
                provider2.acquire(key);
                fail("Should have thrown IllegalStateException due to timeout");
            } catch (IllegalStateException e) {
                long duration = System.currentTimeMillis() - start;
                assertTrue("Timeout was too fast: " + duration, duration >= 1000);
                assertTrue(e.getMessage().contains("ms"));
            } finally {
                releaseThreadA.countDown();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testInterruptionDuringAcquisition() throws Exception {
        String key = "interrupt-key";

        // Mock a situation where the lock is already held
        Resource.Lock firstLock = provider.acquire(key);

        Thread testThread = new Thread(() -> {
            try {
                provider.acquire(key);
                fail("Should have been interrupted");
            } catch (Exception e) {
                // Expected
            }
        });

        testThread.start();
        Thread.sleep(200); // Let it enter the loop
        testThread.interrupt();

        testThread.join(2000);
        assertFalse("Thread should have terminated after interruption", testThread.isAlive());

        firstLock.release();
    }

    /**
     * A thread that already holds the lock for a key must be able to acquire it again without blocking or throwing, and
     * the lock file must still be cleaned up once both acquisitions are released.
     */
    @Test
    public void testReentrantAcquire() throws Exception {
        String key = "nested-key";

        Resource.Lock outer = provider.acquire(key);
        try {
            // must return immediately, not throw OverlappingFileLockException nor wait
            // for the timeout, since the same thread already holds the lock
            Resource.Lock inner = provider.acquire(key);
            inner.release();
        } finally {
            outer.release();
        }

        File lockFile = getLockFile(key);
        await().atMost(2, SECONDS).until(() -> !lockFile.exists());
    }

    /**
     * When a lock is acquired twice on the same thread, releasing only the inner (nested) acquisition must not drop the
     * underlying OS lock; the lock file is only removed once every acquisition on that key has been released.
     */
    @Test
    public void testReentrantAcquireHoldCount() throws Exception {
        String key = "nested-balanced-key";

        Resource.Lock outer = provider.acquire(key);
        Resource.Lock inner = provider.acquire(key);

        File lockFile = getLockFile(key);
        assertTrue("Lock file should still exist while outer hold is active", lockFile.exists());

        inner.release();
        assertTrue("Releasing only the nested acquire must not drop the OS lock", lockFile.exists());

        outer.release();
        await().atMost(2, SECONDS).until(() -> !lockFile.exists());
    }

    /** Locks on different keys must not interfere with each other. */
    @Test
    public void testIndependentKeys() throws Exception {
        Resource.Lock lockA = provider.acquire("key-a");
        Resource.Lock lockB = provider.acquire("key-b");

        assertTrue(getLockFile("key-a").exists());
        assertTrue(getLockFile("key-b").exists());

        lockA.release();
        lockB.release();
    }

    /**
     * Reentrancy must only apply within a single thread: a different thread trying to acquire an already-held key must
     * still block on the gate and eventually time out, exactly as before this change.
     */
    @Test
    public void testConcurrentAcquireOnSameKeyBlocks() throws Exception {
        String key = "contention-key";
        Resource.Lock firstLock = provider.acquire(key);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            long start = System.currentTimeMillis();
            Future<Boolean> future = executor.submit(() -> {
                try {
                    provider.acquire(key);
                    return false; // should not have succeeded while firstLock is held
                } catch (IllegalStateException e) {
                    return true; // expected: timed out waiting for the other thread
                }
            });

            assertTrue("A different thread must not be granted the same key while it is held", future.get(5, SECONDS));
            long duration = System.currentTimeMillis() - start;
            assertTrue("Timeout was too fast: " + duration, duration >= 1000);
        } finally {
            firstLock.release();
            executor.shutdownNow();
        }
    }

    /**
     * A resource can be locked and, while the lock is held, written to and closed on the same thread: closing the
     * output stream re-acquires the same lock key internally (to atomically move the temp file into place), which is
     * exactly the nested-acquire scenario this class must support.
     */
    @Test
    public void testWriteWhileLockHeld() throws Exception {
        FileSystemResourceStore store = new FileSystemResourceStore(root);
        store.setLockProvider(provider);
        Resource resource = store.get("nested/resource.txt");

        Resource.Lock lock = resource.lock();
        try {
            try (OutputStream out = resource.out()) {
                out.write("hello".getBytes(StandardCharsets.UTF_8));
            }
        } finally {
            lock.release();
        }

        assertEquals("hello", IOUtils.toString(resource.in(), StandardCharsets.UTF_8));
    }

    /**
     * A thread wait for the lock, then get it right after the other thread releases it. This thread must then be able
     * to acquire the same lock again, like any other thread that holds the lock.
     */
    @Test
    public void testNestedAcquireAfterWait() throws Exception {
        String key = "waiting-thread-key";
        Resource.Lock outer = provider.acquire(key);

        CountDownLatch waiterStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Throwable> future = executor.submit(() -> {
                waiterStarted.countDown();
                Resource.Lock locked = provider.acquire(key); // blocks until outer releases
                try {
                    Resource.Lock nested = provider.acquire(key); // must not hang or throw
                    nested.release();
                    return null;
                } catch (Throwable t) {
                    return t;
                } finally {
                    locked.release();
                }
            });

            assertTrue("Waiter thread failed to start", waiterStarted.await(2, SECONDS));
            Thread.sleep(300); // make sure the waiter is parked on the gate before releasing
            outer.release();

            Throwable failure = future.get(5, SECONDS);
            assertNull("Nested acquire after handoff must not fail: " + failure, failure);
        } finally {
            executor.shutdownNow();
        }
    }

    private File getLockFile(String key) {
        // This mimics the internal logic of FileLockProvider to verify disk state
        String hash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(key);
        return new File(new File(root, "filelocks"), hash + ".lock");
    }
}
