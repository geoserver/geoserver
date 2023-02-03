/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * If any lifecycle call happens before the shutdown, the required lifecycle beans are already
 * cached and can be used to call onDispose() on each. There was a problem however, that if no such
 * call happened until the final shutdown, the beans could not be created anymore [GEOS-6313]. So,
 * this unit test just starts and stops the geoserver without further actions. The base class
 * verifies, if onDispose() was finally called. To get it run, at least a single dummy test method
 * must be defined.
 */
public class GeoServerLifecycleShutdownTest extends GeoServerLifecycleTestSupport {

    /**
     * This test is just a dummy to turn this class into a unit test. The relevant test happens on
     * destroyGeoServer().
     */
    @Test
    public synchronized void testRunning() {
        assertTrue(applicationContext.isRunning());
    }
}
