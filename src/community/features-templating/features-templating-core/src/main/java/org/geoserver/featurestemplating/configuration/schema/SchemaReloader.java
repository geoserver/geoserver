/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import org.geoserver.config.impl.GeoServerLifecycleHandler;

/**
 * Cleans the cache whenever the reset/reload config is triggered. Mostly useful for tests and REST automation, as the
 * file watcher won't reload files checked less than a second ago.
 */
public class SchemaReloader implements GeoServerLifecycleHandler {

    SchemaLoader loader;

    public SchemaReloader(SchemaLoader configuration) {
        this.loader = configuration;
    }

    @Override
    public void onReset() {
        loader.reset();
    }

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {}

    @Override
    public void onReload() {
        loader.reset();
    }
}
