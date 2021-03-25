/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/** Base Unit tests for AuthenticationCache implementations. */
public abstract class BaseAuthenticationCacheTest {
    protected static final int TIME_LIVE = 2;

    protected static final int TIME_IDLE = 1;

    protected static final int MAX_ENTRIES = 1000;

    protected static final String SAMPLE_CACHE_KEY = "SAMPLE_CACHE_KEY";

    protected static final String SAMPLE_FILTER = "SAMPLE_FILTER";

    protected static final String OTHER_CACHE_KEY = "WRONG_CACHE_KEY";

    AuthenticationCache cache;

    @Before
    public void setUp() {
        cache = createAuthenticationCache();
    }

    protected abstract AuthenticationCache createAuthenticationCache();

    @Test
    public void testWriteAndRead() {
        Authentication auth = putAuthenticationInCache();
        Authentication authenticationFromCache = cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY);
        Assert.assertNotNull(authenticationFromCache);
        Assert.assertEquals(auth, authenticationFromCache);
    }

    @Test
    public void testExpireByAccess() throws InterruptedException {
        putAuthenticationInCache();
        Thread.sleep((TIME_IDLE) * 1000 / 2);
        Assert.assertNotNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
        Thread.sleep((TIME_IDLE + 1) * 1000);
        Assert.assertNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
    }

    @Test
    public void testExpireByCreation() throws InterruptedException {
        putAuthenticationInCache();
        Thread.sleep((TIME_IDLE) * 1000 / 2);
        Assert.assertNotNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
        Thread.sleep((TIME_IDLE) * 1000 / 2);
        Assert.assertNotNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
        Thread.sleep((TIME_IDLE) * 1000 / 2);
        Assert.assertNotNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
        Thread.sleep((TIME_LIVE) * 1000);
        Assert.assertNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
    }

    @Test
    public void testRemoveAuthentication() {
        putAuthenticationInCache();
        cache.remove(SAMPLE_FILTER, SAMPLE_CACHE_KEY);
        Assert.assertNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
    }

    @Test
    public void testRemoveUnexistingAuthentication() {
        cache.remove(SAMPLE_FILTER, OTHER_CACHE_KEY);
        Assert.assertNull(cache.get(SAMPLE_FILTER, OTHER_CACHE_KEY));
    }

    @Test
    public void testRemoveAll() {
        putAuthenticationInCache();
        putOtherAuthenticationInCache();
        Assert.assertNotNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
        Assert.assertNotNull(cache.get(SAMPLE_FILTER, OTHER_CACHE_KEY));
        cache.removeAll();
        Assert.assertNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
        Assert.assertNull(cache.get(SAMPLE_FILTER, OTHER_CACHE_KEY));
    }

    @Test
    public void testRemoveAllByFilter() {
        putAuthenticationInCache();
        putOtherAuthenticationInCache();
        Assert.assertNotNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
        Assert.assertNotNull(cache.get(SAMPLE_FILTER, OTHER_CACHE_KEY));
        cache.removeAll(SAMPLE_FILTER);
        Assert.assertNull(cache.get(SAMPLE_FILTER, SAMPLE_CACHE_KEY));
        Assert.assertNull(cache.get(SAMPLE_FILTER, OTHER_CACHE_KEY));
    }

    protected Authentication putAuthenticationInCache() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "password");
        cache.put(SAMPLE_FILTER, SAMPLE_CACHE_KEY, auth);
        return auth;
    }

    private Authentication putOtherAuthenticationInCache() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user", "password");
        cache.put(SAMPLE_FILTER, OTHER_CACHE_KEY, auth);
        return auth;
    }
}
