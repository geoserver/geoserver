package org.geoserver.jdbcconfig.internal;

import static org.junit.Assert.*;

import com.google.common.cache.Cache;
import java.io.Serializable;
import org.geoserver.util.CacheProvider;
import org.junit.Test;

/**
 * Test class for the {@link JDBCCacheProvider}
 *
 * @author Nicola Lagomarsini geosolutions
 */
public class JDBCCacheProviderTest {

    @Test
    public void testCacheProvider() {
        // Get the provider
        CacheProvider provider = new JDBCCacheProvider();
        // Get the cache
        Cache<Serializable, Serializable> cache = provider.getCache("test");
        // assert if it exists
        assertNotNull(cache);
    }
}
