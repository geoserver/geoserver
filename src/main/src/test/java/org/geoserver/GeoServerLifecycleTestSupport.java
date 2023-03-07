/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import static org.junit.Assert.assertTrue;

import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Base test class for GeoServer lifecycle tests to verify different {@link
 * GeoServerLifecycleHandler} calls.
 *
 * <p>Subclasses may ask the internal {@link LifecycleWatcher} if the expected call took place. A
 * special case is the onDispose() call, which can not be addressed by a unit test. Therefor an
 * assertion was placed in the destroyGeoServer() hook.
 *
 * @author d.stueken (con terra)
 */
public class GeoServerLifecycleTestSupport extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData data) throws Exception {
        super.onSetUp(data);
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        beanFactory.registerSingleton("lifecycleWatcher", new LifecycleWatcher());
    }

    protected LifecycleWatcher getLifecycleWatcher() {
        return applicationContext.getBean(LifecycleWatcher.class);
    }

    @Override
    protected void destroyGeoServer() {
        LifecycleWatcher watcher = getLifecycleWatcher().reset();
        super.destroyGeoServer();
        // verify if onDispose() was finally called.
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
