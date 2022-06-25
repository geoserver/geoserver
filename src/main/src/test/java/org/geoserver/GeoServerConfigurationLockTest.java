/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.geoserver;

import static org.geoserver.GeoServerConfigurationLock.LockType.READ;
import static org.geoserver.GeoServerConfigurationLock.LockType.WRITE;
import static org.geoserver.GeoServerConfigurationLock.TRYLOCK_TIMEOUT_SYSTEM_PROPERTY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Hint: all tests are annotated with {@code @Test(timeout = 1000)}. If one fails, will most
 * probably result in a cascade failure, so look at the first one that failed or the immediate
 * previous one that succeeded in case it didn't unlock and fix it.
 */
public class GeoServerConfigurationLockTest {

    private final GeoServerConfigurationLock lock = new GeoServerConfigurationLock();

    @Before
    public void beforeEach() {
        System.setProperty(TRYLOCK_TIMEOUT_SYSTEM_PROPERTY, "100");
    }

    @After
    public void afterEach() {
        // //abuse the idempotency of unlock() to clean up after a failed test
        // lock.unlock();
        System.clearProperty(TRYLOCK_TIMEOUT_SYSTEM_PROPERTY);
        assertFalse("all locks shall have been released", lock.isWriteLocked());
    }

    @Test(timeout = 1000)
    public void testLock_WriteLock() {
        assertNull(lock.getCurrentLock());
        lock.lock(WRITE);
        assertEquals(WRITE, lock.getCurrentLock());
        lock.unlock();
        assertNull(lock.getCurrentLock());
    }

    @Test(timeout = 1000)
    public void testLock_ReadLock() {
        assertNull(lock.getCurrentLock());
        lock.lock(READ);
        assertEquals(READ, lock.getCurrentLock());
        lock.unlock();
        assertNull(lock.getCurrentLock());
    }

    @Test(timeout = 1000)
    public void testLock_ReadLock_preserves_write_lock_if_alread_held() {
        assertNull(lock.getCurrentLock());
        lock.lock(WRITE);
        assertEquals(WRITE, lock.getCurrentLock());

        lock.lock(READ);
        assertEquals(
                "A read lock request shall preserve the write lock if already held",
                WRITE,
                lock.getCurrentLock());
        lock.unlock();
    }

    @Test(timeout = 1000)
    public void testTryUpgradeLock_fais_if_no_previous_lock_is_held() {
        assertNull(lock.getCurrentLock());
        IllegalStateException ex = assertThrows(IllegalStateException.class, lock::tryUpgradeLock);
        assertThat(ex.getMessage(), containsString("No lock currently held"));
    }

    @Test(timeout = 1000)
    public void testTryUpgradeLock_fails_if_already_holds_a_write_lock() {
        assertNull(lock.getCurrentLock());
        lock.lock(WRITE);

        IllegalStateException ex = assertThrows(IllegalStateException.class, lock::tryUpgradeLock);
        assertThat(ex.getMessage(), containsString("Already owning a write lock"));
        // this case, contrary to when a read lock is held, but tryUpgradeLock() fails, does not
        // release the currently held lock
        assertEquals(WRITE, lock.getCurrentLock());
        lock.unlock();
    }

    @Test(timeout = 1000)
    public void testTryUpgradeLock() throws InterruptedException, ExecutionException {
        ExecutorService secondThread = Executors.newSingleThreadExecutor();
        try {
            lock.lock(READ);

            secondThread
                    .submit(
                            () -> {
                                assertTrue(lock.tryLock(READ));
                            })
                    .get();

            assertEquals(READ, lock.getCurrentLock());

            RuntimeException ex = assertThrows(RuntimeException.class, () -> lock.tryUpgradeLock());
            assertThat(
                    ex.getMessage(),
                    containsString("Failed to upgrade lock from read to write state"));

            assertNull(
                    "lock should have been lost after a failed tryUpgradeLock()",
                    lock.getCurrentLock());

            lock.lock(READ);

            secondThread.submit(lock::unlock).get();

            lock.tryUpgradeLock();
            assertEquals(WRITE, lock.getCurrentLock());
        } finally {
            secondThread.shutdownNow();
            lock.unlock();
        }
    }

    @Test(timeout = 1000)
    public void testTryLock() {
        assertTrue(lock.tryLock(READ));
        assertEquals(READ, lock.getCurrentLock());
        lock.unlock();
        assertNull(lock.getCurrentLock());

        assertTrue(lock.tryLock(WRITE));
        assertEquals(WRITE, lock.getCurrentLock());
        lock.unlock();
        assertNull(lock.getCurrentLock());
    }

    @Test(timeout = 1000)
    public void testTryLock_false_if_write_lock_requested_while_holding_a_read_lock() {
        assertNull(lock.getCurrentLock());

        assertTrue(lock.tryLock(READ));
        assertEquals(READ, lock.getCurrentLock());

        assertFalse(lock.tryLock(WRITE));
        assertEquals(READ, lock.getCurrentLock());
        lock.unlock();
        assertNull(lock.getCurrentLock());
    }

    @Test(timeout = 1000)
    public void testTryLock_true_if_read_lock_requested_while_holding_a_write_lock() {
        assertTrue(lock.tryLock(WRITE));
        assertEquals(WRITE, lock.getCurrentLock());

        assertTrue(lock.tryLock(READ));
        assertEquals(
                "tryLock(READ) while holding a write lock shall preserve the write lock",
                WRITE,
                lock.getCurrentLock());

        lock.unlock();
        assertNull(lock.getCurrentLock());
        assertFalse(lock.isWriteLocked());
    }

    @Test(timeout = 1000)
    public void testUnlock() {
        assertNull(lock.getCurrentLock());
        lock.unlock();
        lock.unlock();
        assertNull(lock.getCurrentLock());

        lock.lock(READ);
        lock.unlock();
        assertNull(lock.getCurrentLock());

        lock.lock(WRITE);
        lock.unlock();
        assertNull(lock.getCurrentLock());
    }

    @Test(timeout = 1000)
    public void testLock_ReadLockIsReentrant() {
        testLockIsReentrant(READ);
    }

    @Test(timeout = 1000)
    public void testLock_WriteLockIsReentrant() {
        testLockIsReentrant(WRITE);
    }

    private void testLockIsReentrant(LockType lockType) {
        assertNull(lock.getCurrentLock());
        // first time
        lock.lock(lockType);
        try {
            assertEquals(lockType, lock.getCurrentLock());
            try {
                // second acquire
                lock.lock(lockType);
                assertEquals(lockType, lock.getCurrentLock());
            } finally {
                // first release
                lock.unlock();
                assertEquals(
                        lockType + " lock should still be held", lockType, lock.getCurrentLock());
            }
        } finally {
            // second release
            lock.unlock();
            assertNull(lock.getCurrentLock());
        }
        assertFalse(lock.isWriteLocked());
    }

    @Test(timeout = 1000)
    public void testTryReadLockIsReentrant() {
        testTryLockIsReentrant(READ);
    }

    @Test(timeout = 1000)
    public void testTryWriteLockIsReentrant() {
        testTryLockIsReentrant(WRITE);
    }

    private void testTryLockIsReentrant(final LockType lockType) {
        assertNull(lock.getCurrentLock());
        // common case scenario from nested calls:

        // first time
        try {
            assertTrue(lock.tryLock(lockType));
            assertEquals(lockType, lock.getCurrentLock());
            try {
                assertTrue(lock.tryLock(lockType));
                assertEquals(lockType, lock.getCurrentLock());
            } finally {
                // first release
                lock.unlock();
                assertEquals(
                        lockType + " lock should still be held", lockType, lock.getCurrentLock());
            }
        } finally {
            // second release
            lock.unlock();
            assertNull(lock.getCurrentLock());
        }
    }
}
