/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static org.junit.Assert.*;

import org.junit.Test;

public class GeoServerLifecycleTests extends GeoServerLifecycleTest {

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
