/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.mock.web.MockHttpServletResponse;

public class CatalogReloadControllerTest extends GeoServerSystemTestSupport {

    static ReloadResetWatcher watcher = new ReloadResetWatcher();

    @Override
    protected void onSetUp(SystemTestData data) {
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        beanFactory.registerSingleton("ReloadResetWatcher", watcher);
    }

    public void reset() {
        watcher.didReload = false;
        watcher.didReset = false;
    }

    @Test
    public synchronized void testPutReload() throws Exception {
        reset();
        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/reload", (String) null, null);
        assertEquals(200, response.getStatus());
        assertTrue(watcher.didReload);
        assertTrue(watcher.didReset);
    }

    @Test
    public synchronized void testPostReload() throws Exception {
        reset();
        MockHttpServletResponse response = postAsServletResponse(ROOT_PATH + "/reload", "", null);
        assertEquals(200, response.getStatus());
        assertTrue(watcher.didReload);
        assertTrue(watcher.didReset);
    }

    @Test
    public synchronized void testPutReset() throws Exception {
        reset();
        MockHttpServletResponse response =
                putAsServletResponse(ROOT_PATH + "/reset", (String) null, null);
        assertEquals(200, response.getStatus());
        assertFalse(watcher.didReload);
        assertTrue(watcher.didReset);
    }

    @Test
    public synchronized void testPostReset() throws Exception {
        reset();
        MockHttpServletResponse response = postAsServletResponse(ROOT_PATH + "/reset", "", null);
        assertEquals(200, response.getStatus());
        assertFalse(watcher.didReload);
        assertTrue(watcher.didReset);
    }

    private static class ReloadResetWatcher implements GeoServerLifecycleHandler {

        boolean didReload;
        boolean didReset;

        @Override
        public void onReset() {
            didReset = true;
        }

        @Override
        public void onReload() {
            didReload = true;
        }

        @Override
        public void onDispose() {}

        @Override
        public void beforeReload() {}
    }
}
