/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import org.geoserver.config.impl.GeoServerLifecycleHandler;

/**
 * Cleans the cache whenever the reset/reload config is triggered. Mostly useful for tests and REST
 * automation, as the file watcher won't reload files checked less than a second ago.
 */
public class TemplateReloader implements GeoServerLifecycleHandler {

    TemplateConfiguration configuration;

    public TemplateReloader(TemplateConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onReset() {
        configuration.reset();
    }

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {}

    @Override
    public void onReload() {
        configuration.reset();
    }
}
