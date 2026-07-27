/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AdminRequest;
import org.geoserver.security.impl.SecurityFilterCache.FilterLoader;
import org.geoserver.security.impl.SecurityFilterCache.SecurityFilterCacheKey;
import org.geotools.api.filter.Filter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

public class SecurityFilterCacheTest {

    private SecurityFilterCache cache;
    private TestingAuthenticationToken user;

    @Before
    public void setUp() {
        user = new TestingAuthenticationToken("rwUser", "password");

        FilterLoader loader = (authentication, type, adminRequest) -> createNiceMock(Filter.class);
        Cache<SecurityFilterCacheKey, Filter> backingCache =
                CacheBuilder.newBuilder().maximumSize(10).build();
        cache = new SecurityFilterCache(backingCache, loader);
    }

    /**
     * Verifies that the cache reuses the same security filter for the same user, catalog type, and admin-request state.
     */
    @Test
    public void testReusesCachedFilterForSameKey() {
        // Build the filter once for the normal request context.
        Filter first = cache.get(user, WorkspaceInfo.class, false);

        // Ask for the same key again and expect the cached filter back.
        Filter second = cache.get(user, WorkspaceInfo.class, false);

        assertSame(first, second);
    }

    /**
     * Verifies that admin-request state participates in the cache key, so the same user can receive a different
     * workspace filter while AdminRequest is active.
     */
    @Test
    public void testSeparatesAdminRequestEntries() {
        // Build the non-admin filter first.
        Filter normal = cache.get(user, WorkspaceInfo.class, false);

        // Build the admin-request filter for the same user and catalog type.
        Filter admin = cache.get(user, WorkspaceInfo.class, true);

        assertNotSame(normal, admin);
    }

    /** Verifies that invalidation clears the memoized entry and forces the next lookup to reload the filter. */
    @Test
    public void testInvalidateAllForcesReload() {
        // Load the initial filter.
        Filter first = cache.get(user, WorkspaceInfo.class, false);

        // Clear the cache to simulate a rules reload.
        cache.invalidateAll();

        // The next lookup should build a new filter instance.
        Filter second = cache.get(user, WorkspaceInfo.class, false);

        assertNotSame(first, second);
    }

    /**
     * Verifies that the cache passes the explicit admin-request flag to the loader instead of relying on ambient
     * thread-local state.
     */
    @Test
    public void testLoaderReceivesExplicitAdminRequestFlag() {
        AtomicBoolean loaderSawAdminRequest = new AtomicBoolean(true);
        Filter normalFilter = createNiceMock(Filter.class);
        Filter adminFilter = createNiceMock(Filter.class);

        FilterLoader loader = (authentication, type, adminRequest) -> {
            loaderSawAdminRequest.set(adminRequest);
            return adminRequest ? adminFilter : normalFilter;
        };
        Cache<SecurityFilterCacheKey, Filter> backingCache =
                CacheBuilder.newBuilder().maximumSize(10).build();
        cache = new SecurityFilterCache(backingCache, loader);

        // Put a conflicting AdminRequest state on the thread-local to prove the loader uses the explicit flag.
        AdminRequest.start(new Object());
        try {
            Filter filter = cache.get(user, WorkspaceInfo.class, false);

            assertSame(normalFilter, filter);
            assertFalse(loaderSawAdminRequest.get());
        } finally {
            AdminRequest.finish();
        }

        // Verify the loader receives the explicit admin-request flag when the cache key changes.
        Filter adminResult = cache.get(user, WorkspaceInfo.class, true);

        assertSame(adminFilter, adminResult);
        assertTrue(loaderSawAdminRequest.get());
    }

    /**
     * Verifies that concurrent cache population is thread-safe: one thread loads the filter while the racing thread
     * reuses the same cached value.
     */
    @Test
    public void testConcurrentLoadUsesSingleBuiltValue() throws Exception {
        AtomicInteger loadCount = new AtomicInteger(0);
        CountDownLatch loaderEntered = new CountDownLatch(1);
        CountDownLatch allowLoaderToFinish = new CountDownLatch(1);
        AtomicReference<Filter> firstResult = new AtomicReference<>();
        AtomicReference<Filter> secondResult = new AtomicReference<>();
        Filter builtFilter = createNiceMock(Filter.class);

        FilterLoader loader = (authentication, type, adminRequest) -> {
            loadCount.incrementAndGet();
            loaderEntered.countDown();
            try {
                if (!allowLoaderToFinish.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to release the loader");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for the loader gate", e);
            }
            return builtFilter;
        };
        Cache<SecurityFilterCacheKey, Filter> backingCache =
                CacheBuilder.newBuilder().maximumSize(10).build();
        cache = new SecurityFilterCache(backingCache, loader);

        Thread firstThread = new Thread(() -> firstResult.set(cache.get(user, WorkspaceInfo.class, false)));
        Thread secondThread = new Thread(() -> {
            secondResult.set(cache.get(user, WorkspaceInfo.class, false));
        });

        firstThread.start();
        assertTrue("Loader never started", loaderEntered.await(10, TimeUnit.SECONDS));

        secondThread.start();
        assertTrue("Second thread never blocked in cache.get()", waitForBlockedState(secondThread));

        allowLoaderToFinish.countDown();

        firstThread.join(10_000);
        secondThread.join(10_000);

        assertFalse("First thread still running", firstThread.isAlive());
        assertFalse("Second thread still running", secondThread.isAlive());
        assertSame(builtFilter, firstResult.get());
        assertSame(builtFilter, secondResult.get());
        assertEquals(1, loadCount.get());
    }

    private boolean waitForBlockedState(Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Thread.State state = thread.getState();
            if (state == Thread.State.BLOCKED || state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                return true;
            }
            Thread.sleep(10L);
        }
        return false;
    }
}
