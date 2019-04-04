/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class LRUAuthenticationCacheTest extends BaseAuthenticationCacheTest {

    @Test
    public void testLRUCache() {

        LRUCache<String, String> cache = new LRUCache<String, String>(3);
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4");

        assertEquals("value2", cache.get("key2"));
        assertEquals("value3", cache.get("key3"));
        assertEquals("value4", cache.get("key4"));
        assertNull(cache.get("key1"));
    }

    @Test
    public void testAuthenticationKey() {
        AuthenticationCacheKey key11 = new AuthenticationCacheKey("f1", "k1");
        assertTrue(key11.equals(key11));
        assertTrue(key11.hashCode() != 0);

        AuthenticationCacheKey key12 = new AuthenticationCacheKey("f1", "k2");
        assertFalse(key11.equals(key12));
        assertFalse(key11.hashCode() == key12.hashCode());

        AuthenticationCacheKey key21 = new AuthenticationCacheKey("f2", "k1");
        assertFalse(key11.equals(key21));
        assertFalse(key11.hashCode() == key21.hashCode());

        AuthenticationCacheKey key22 = new AuthenticationCacheKey("f12", "k2");
        assertFalse(key11.equals(key22));
        assertFalse(key11.hashCode() == key22.hashCode());
    }

    @Test
    public void testAuthenticationEntry() {

        UsernamePasswordAuthenticationToken t1 =
                new UsernamePasswordAuthenticationToken("user1", "password1");
        AuthenticationCacheEntry entry1 = new AuthenticationCacheEntry(t1, 10, 10);
        assertTrue(entry1.hashCode() != 0);
        assertEquals(t1.hashCode(), entry1.hashCode());
        assertTrue(entry1.equals(entry1));

        AuthenticationCacheEntry entry1_1 = new AuthenticationCacheEntry(t1, 20, 20);
        assertEquals(t1.hashCode(), entry1_1.hashCode());
        assertTrue(entry1.equals(entry1_1));

        UsernamePasswordAuthenticationToken t2 =
                new UsernamePasswordAuthenticationToken("user2", "password2");
        AuthenticationCacheEntry entry2 = new AuthenticationCacheEntry(t2, 5, 10);
        // assertFalse(entry2.hashCode()==entry1.hashCode());
        assertFalse(entry2.equals(entry1));

        long currentTime = entry2.getCreated();
        // check live time
        entry2.setLastAccessed(currentTime + 6000);
        assertFalse(entry2.hasExpired(currentTime + 10 * 1000));
        assertTrue(entry2.hasExpired(currentTime + 10 * 1000 + 1));

        // check idle time
        entry2.setLastAccessed(currentTime + 2000);
        assertFalse(entry2.hasExpired(currentTime + 7000));
        assertTrue(entry2.hasExpired(currentTime + 70001));
    }

    protected void fillCache(AuthenticationCache cache) {
        UsernamePasswordAuthenticationToken t1 =
                new UsernamePasswordAuthenticationToken("user1", "password1");
        UsernamePasswordAuthenticationToken t2 =
                new UsernamePasswordAuthenticationToken("user2", "password2");
        UsernamePasswordAuthenticationToken t3 =
                new UsernamePasswordAuthenticationToken("user3", "password3");
        UsernamePasswordAuthenticationToken t4 =
                new UsernamePasswordAuthenticationToken("user4", "password4");

        cache.put("filtera", "key1", t1);
        cache.put("filtera", "key2", t2);
        cache.put("filterb", "key3", t3);
        cache.put("filterb", "key4", t4);
    }

    protected void waitForMilliSecs(long milliSecs) {
        try {
            Thread.sleep(milliSecs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLRUAuthenticationCache() {
        // test max entries
        LRUAuthenticationCacheImpl cache = new LRUAuthenticationCacheImpl(5, 10, 3);
        fillCache(cache);

        UsernamePasswordAuthenticationToken token;
        assertNull(cache.get("filtera", "key1"));
        assertNotNull(token = (UsernamePasswordAuthenticationToken) cache.get("filtera", "key2"));
        assertEquals("user2", token.getPrincipal());
        assertNotNull(token = (UsernamePasswordAuthenticationToken) cache.get("filterb", "key3"));
        assertEquals("user3", token.getPrincipal());
        assertNotNull(token = (UsernamePasswordAuthenticationToken) cache.get("filterb", "key4"));
        assertEquals("user4", token.getPrincipal());

        // test remove all
        cache = new LRUAuthenticationCacheImpl(5, 10, 4);
        fillCache(cache);
        cache.removeAll();
        assertNull(cache.get("filtera", "key1"));
        assertNull(cache.get("filtera", "key2"));
        assertNull(cache.get("filterb", "key3"));
        assertNull(cache.get("filterb", "key4"));

        // test remove filter
        cache = new LRUAuthenticationCacheImpl(5, 10, 4);
        fillCache(cache);
        cache.removeAll("filtera");
        assertNull(cache.get("filtera", "key1"));
        assertNull(cache.get("filtera", "key2"));
        assertNotNull(cache.get("filterb", "key3"));
        assertNotNull(cache.get("filterb", "key4"));

        // test remove one entry
        cache = new LRUAuthenticationCacheImpl(5, 10, 4);
        fillCache(cache);
        cache.remove("filtera", "key1");
        assertNull(cache.get("filtera", "key1"));
        assertNotNull(cache.get("filtera", "key2"));
        assertNotNull(cache.get("filterb", "key3"));
        assertNotNull(cache.get("filterb", "key4"));

        // test remove non existing
        cache = new LRUAuthenticationCacheImpl(5, 10, 4);
        fillCache(cache);
        cache.removeAll("filterz");
        cache.remove("filterz", "key999");
        assertNotNull(cache.get("filtera", "key1"));
        assertNotNull(cache.get("filtera", "key2"));
        assertNotNull(cache.get("filterb", "key3"));
        assertNotNull(cache.get("filterb", "key4"));

        // test default live time
        cache = new LRUAuthenticationCacheImpl(5, 0, 4);
        fillCache(cache);
        waitForMilliSecs(10);
        assertNull(cache.get("filtera", "key1"));
        assertNull(cache.get("filtera", "key2"));
        assertNull(cache.get("filterb", "key3"));
        assertNull(cache.get("filterb", "key4"));

        // test default idle time
        cache = new LRUAuthenticationCacheImpl(0, 10, 4);
        fillCache(cache);
        waitForMilliSecs(10);
        assertNull(cache.get("filtera", "key1"));
        assertNull(cache.get("filtera", "key2"));
        assertNull(cache.get("filterb", "key3"));
        assertNull(cache.get("filterb", "key4"));

        cache = new LRUAuthenticationCacheImpl(1, 10, 4);
        fillCache(cache);
        waitForMilliSecs(1);
        assertNotNull(cache.get("filtera", "key1"));
        assertNotNull(cache.get("filtera", "key2"));
        assertNotNull(cache.get("filterb", "key3"));
        assertNotNull(cache.get("filterb", "key4"));

        waitForMilliSecs(1500);
        assertNull(cache.get("filtera", "key1"));
        assertNull(cache.get("filtera", "key2"));
        assertNull(cache.get("filterb", "key3"));
        assertNull(cache.get("filterb", "key4"));
    }

    @Override
    protected AuthenticationCache createAuthenticationCache() {
        return new LRUAuthenticationCacheImpl(TIME_IDLE, TIME_LIVE, MAX_ENTRIES);
    }
}
