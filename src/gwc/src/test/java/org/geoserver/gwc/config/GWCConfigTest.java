/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.storage.blobstore.memory.guava.GuavaCacheProvider;
import org.junit.Before;
import org.junit.Test;

public class GWCConfigTest extends GeoServerSystemTestSupport {

    private GWCConfig oldDefaults;

    private GWCConfig config;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Before
    public void setup() throws Exception {
        oldDefaults = GWCConfig.getOldDefaults();
        config = new GWCConfig();
    }

    @Test
    public void testSaneConfig() {
        assertTrue(config.isSane());
        assertSame(config, config.saneConfig());
        assertTrue(oldDefaults.isSane());
        assertSame(oldDefaults, oldDefaults.saneConfig());

        config.setMetaTilingX(-1);
        assertFalse(config.isSane());
        assertTrue((config = config.saneConfig()).isSane());

        config.setMetaTilingY(-1);
        assertFalse(config.isSane());
        assertTrue((config = config.saneConfig()).isSane());

        config.setGutter(-1);
        assertFalse(config.isSane());
        assertTrue((config = config.saneConfig()).isSane());

        config.getDefaultCachingGridSetIds().clear();
        assertFalse(config.isSane());
        assertTrue((config = config.saneConfig()).isSane());

        config.getDefaultCoverageCacheFormats().clear();
        assertFalse(config.isSane());
        assertTrue((config = config.saneConfig()).isSane());

        config.getDefaultOtherCacheFormats().clear();
        assertFalse(config.isSane());
        assertTrue((config = config.saneConfig()).isSane());

        config.getDefaultVectorCacheFormats().clear();
        assertFalse(config.isSane());
        assertTrue((config = config.saneConfig()).isSane());
    }

    @Test
    public void testClone() {
        GWCConfig clone = config.clone();
        assertEquals(config, clone);
        assertNotSame(config.getDefaultCachingGridSetIds(), clone.getDefaultCachingGridSetIds());
        assertNotSame(
                config.getDefaultCoverageCacheFormats(), clone.getDefaultCoverageCacheFormats());
        assertNotSame(config.getDefaultOtherCacheFormats(), clone.getDefaultOtherCacheFormats());
        assertNotSame(config.getDefaultVectorCacheFormats(), clone.getDefaultVectorCacheFormats());
        assertNotSame(config.getCacheConfigurations(), clone.getCacheConfigurations());
        assertTrue(clone.getCacheConfigurations().containsKey(GuavaCacheProvider.class.toString()));
    }

    @Test
    public void testIsServiceEnabled() {
        config.setWMSCEnabled(!config.isWMSCEnabled());
        config.setTMSEnabled(!config.isTMSEnabled());

        assertEquals(config.isEnabled("wms"), config.isWMSCEnabled());
        assertEquals(config.isEnabled("WMS"), config.isWMSCEnabled());
        assertEquals(config.isEnabled("tms"), config.isTMSEnabled());
        assertEquals(config.isEnabled("TMS"), config.isTMSEnabled());

        assertTrue(config.isEnabled("anything else"));
    }
}
