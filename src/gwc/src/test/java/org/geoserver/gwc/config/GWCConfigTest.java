/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import junit.framework.TestCase;

public class GWCConfigTest extends TestCase {

    private GWCConfig oldDefaults;

    private GWCConfig config;

    protected void setUp() throws Exception {
        oldDefaults = GWCConfig.getOldDefaults();
        config = new GWCConfig();
    }

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

    public void testClone() {
        GWCConfig clone = config.clone();
        assertEquals(config, clone);
        assertNotSame(config.getDefaultCachingGridSetIds(), clone.getDefaultCachingGridSetIds());
        assertNotSame(config.getDefaultCoverageCacheFormats(),
                clone.getDefaultCoverageCacheFormats());
        assertNotSame(config.getDefaultOtherCacheFormats(), clone.getDefaultOtherCacheFormats());
        assertNotSame(config.getDefaultVectorCacheFormats(), clone.getDefaultVectorCacheFormats());
    }

    public void testIsServiceEnabled() {
        config.setWMSCEnabled(!config.isWMSCEnabled());
        config.setTMSEnabled(!config.isTMSEnabled());
        config.setWMTSEnabled(!config.isWMTSEnabled());

        assertEquals(config.isEnabled("wms"), config.isWMSCEnabled());
        assertEquals(config.isEnabled("WMS"), config.isWMSCEnabled());
        assertEquals(config.isEnabled("wmts"), config.isWMTSEnabled());
        assertEquals(config.isEnabled("WMTS"), config.isWMTSEnabled());
        assertEquals(config.isEnabled("tms"), config.isTMSEnabled());
        assertEquals(config.isEnabled("TMS"), config.isTMSEnabled());

        assertTrue(config.isEnabled("anything else"));
    }
}
