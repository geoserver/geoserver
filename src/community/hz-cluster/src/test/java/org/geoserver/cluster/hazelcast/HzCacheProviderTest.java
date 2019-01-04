/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import static org.junit.Assert.assertNotNull;

import com.google.common.cache.Cache;
import java.io.Serializable;
import org.geoserver.catalog.Info;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.junit.Before;
import org.junit.Test;

public class HzCacheProviderTest {

    private HzCacheProvider cacheProvider;

    @Before
    public void createCacheProvider() {
        this.cacheProvider = new HzCacheProvider(new XStreamPersisterFactory());
    }

    @Test
    public void testGetCache() {
        Cache<Serializable, Serializable> cache = this.cacheProvider.getCache("test");
        assertNotNull(cache);
    }

    @Test
    public void testGetCacheCatalog() {
        Cache<String, Info> cache = this.cacheProvider.getCache("catalog");
        assertNotNull(cache);
    }
}
