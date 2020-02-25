/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

/** Unit tests for Guava based AuthenticationCache implementation. */
public class GuavaAuthenticationCacheTest extends BaseAuthenticationCacheTest {
    private static final int CONCURRENCY = 3;

    protected static final int TIME_CLEANUP = 3;

    @Override
    protected AuthenticationCache createAuthenticationCache() {
        return new GuavaAuthenticationCacheImpl(
                MAX_ENTRIES, TIME_IDLE, TIME_LIVE, TIME_CLEANUP, CONCURRENCY);
    }

    public void testCleanUp() throws InterruptedException {
        putAuthenticationInCache();
        Thread.sleep((TIME_CLEANUP + 1) * 1000);
        assertTrue(((GuavaAuthenticationCacheImpl) cache).isEmpty());
    }
}
