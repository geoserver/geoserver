/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static org.junit.Assert.assertTrue;

import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class GeoServerLifecycleTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData data) throws Exception {
        super.onSetUp(data);
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        beanFactory.registerSingleton("lifecycleWatcher", new LifecycleWatcher());
    }

    protected LifecycleWatcher getLifecycleWatcher() {
        return applicationContext.getBean(LifecycleWatcher.class);
    }

    @Test
    public synchronized void testRunning() {
        assertTrue(applicationContext.isRunning());
    }

    @Override
    protected void destroyGeoServer() {
        LifecycleWatcher watcher = getLifecycleWatcher().reset();
        super.destroyGeoServer();
        assertTrue(watcher.didDispose);
    }

    protected static class LifecycleWatcher implements GeoServerLifecycleHandler {

        boolean didReload = false;

        boolean didBeforeReload = false;

        boolean didReset = false;

        boolean didDispose = false;

        public LifecycleWatcher reset() {
            didBeforeReload = false;
            didReload = false;
            didReset = false;
            didDispose = false;

            return this;
        }

        @Override
        public void onReset() {
            didReset = true;
        }

        @Override
        public void onReload() {
            didReload = true;
        }

        @Override
        public void onDispose() {
            didDispose = true;
        }

        @Override
        public void beforeReload() {
            didBeforeReload = true;
        }
    }
}
