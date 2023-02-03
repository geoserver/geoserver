/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test if all callbacks on {@link LifecycleWatcher} took place
 *
 * @author d.stueken (con terra)
 */
public class GeoServerLifecycleTests extends GeoServerLifecycleTestSupport {

    @Test
    public void testReset() {
        LifecycleWatcher watcher = getLifecycleWatcher().reset();
        getGeoServer().reset();
        assertTrue(watcher.didReset);
    }

    @Test
    public void testReload() throws Exception {
        LifecycleWatcher watcher = getLifecycleWatcher().reset();
        getGeoServer().reload();
        assertTrue(watcher.didBeforeReload);
        assertTrue(watcher.didReload);
    }
}
