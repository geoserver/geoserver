/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.coverage;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.easymock.EasyMock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geotools.gce.imagemosaic.GranuleImageCache;
import org.junit.Test;

public class CoverageAccessInitializerTest {

    @Test
    public void testChangePoolSize() throws Exception {
        // start point
        CoverageAccessInfoImpl caInfo = new CoverageAccessInfoImpl();
        caInfo.setCorePoolSize(5);
        caInfo.setMaxPoolSize(5);
        GeoServerInfoImpl gsInfo = new GeoServerInfoImpl();
        gsInfo.setCoverageAccess(caInfo);

        GeoServer gs = EasyMock.createNiceMock(GeoServer.class);
        Catalog catalog = EasyMock.createNiceMock(Catalog.class);
        ResourcePool rp = ResourcePool.create(catalog);
        expect(gs.getCatalog()).andReturn(catalog).anyTimes();
        expect(gs.getGlobal()).andReturn(gsInfo).anyTimes();
        expect(catalog.getResourcePool()).andReturn(rp).anyTimes();
        replay(gs);
        replay(catalog);

        CoverageAccessInitializer initializer = new CoverageAccessInitializer();
        initializer.initialize(gs);
        assertEquals(5, rp.getCoverageExecutor().getCorePoolSize());
        assertEquals(5, rp.getCoverageExecutor().getMaximumPoolSize());

        // going up
        caInfo.setCorePoolSize(32);
        caInfo.setMaxPoolSize(32);
        initializer.initialize(gs);
        assertEquals(32, rp.getCoverageExecutor().getCorePoolSize());
        assertEquals(32, rp.getCoverageExecutor().getMaximumPoolSize());

        // and back down
        caInfo.setCorePoolSize(5);
        caInfo.setMaxPoolSize(5);
        initializer.initialize(gs);
        assertEquals(5, rp.getCoverageExecutor().getCorePoolSize());
        assertEquals(5, rp.getCoverageExecutor().getMaximumPoolSize());
    }

    @Test
    public void testGranuleImageCacheLifecycle() throws Exception {
        CoverageAccessInfoImpl caInfo = new CoverageAccessInfoImpl();
        GeoServerInfoImpl gsInfo = new GeoServerInfoImpl();
        gsInfo.setCoverageAccess(caInfo);

        GeoServer gs = EasyMock.createNiceMock(GeoServer.class);
        Catalog catalog = EasyMock.createNiceMock(Catalog.class);
        ResourcePool rp = ResourcePool.create(catalog);
        expect(gs.getCatalog()).andReturn(catalog).anyTimes();
        expect(gs.getGlobal()).andReturn(gsInfo).anyTimes();
        expect(catalog.getResourcePool()).andReturn(rp).anyTimes();
        replay(gs);
        replay(catalog);

        CoverageAccessInitializer initializer = new CoverageAccessInitializer();

        // unconfigured (null fields): caching is on with the 128 MB / 1024 KB defaults, staying idle until a
        // mosaic opts in via its read parameters
        initializer.initialize(gs);
        GranuleImageCache cache = rp.getGranuleImageCache();
        assertNotNull(cache);
        assertTrue(cache.isEnabled());
        assertEquals(128L * 1024 * 1024, cache.getMaximumSizeBytes());
        assertEquals(1024L * 1024, cache.getDefaultThresholdBytes());

        // enabling reconfigures the same instance, so readers holding it are never rebuilt
        caInfo.setGranuleCacheMaxSizeMB(64);
        caInfo.setGranuleCacheThresholdKB(512);
        initializer.initialize(gs);
        assertSame(cache, rp.getGranuleImageCache());
        assertTrue(cache.isEnabled());
        assertEquals(64L * 1024 * 1024, cache.getMaximumSizeBytes());
        assertEquals(512L * 1024, cache.getDefaultThresholdBytes());

        // threshold change in place
        caInfo.setGranuleCacheThresholdKB(256);
        initializer.initialize(gs);
        assertSame(cache, rp.getGranuleImageCache());
        assertEquals(256L * 1024, cache.getDefaultThresholdBytes());

        // size change in place, same instance, new budget
        caInfo.setGranuleCacheMaxSizeMB(128);
        initializer.initialize(gs);
        assertSame(cache, rp.getGranuleImageCache());
        assertEquals(128L * 1024 * 1024, cache.getMaximumSizeBytes());

        // disabling keeps the instance but turns caching off and zeroes the budget so it cannot retain anything
        caInfo.setGranuleCacheMaxSizeMB(0);
        initializer.initialize(gs);
        assertSame(cache, rp.getGranuleImageCache());
        assertFalse(cache.isEnabled());
    }
}
