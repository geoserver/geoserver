/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind.config;

import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.platform.GeoServerResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class responsible for loading and holding the current PNG-WIND configuration, and reloading it when
 * GeoServer is reloaded or reset allowing to edit the config without the need of restarting the server. The
 * configuration is loaded from a properties file (pngwind.properties)
 */
public class PngWindConfigurator implements GeoServerLifecycleHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PngWindConfigurator.class);

    private static volatile PngWindConfigurator INSTANCE;

    private volatile PngWindConfig config;

    private final GeoServerResourceLoader loader;

    private PngWindConfigurator(GeoServerResourceLoader loader) {
        this.loader = loader;
        INSTANCE = this;
    }

    public static PngWindConfig getCurrentConfig() {
        if (INSTANCE == null || INSTANCE.config == null) {
            throw new IllegalStateException("PNG-WIND configuration not initialized");
        }
        return INSTANCE.config;
    }

    @Override
    public void onReset() {
        reload();
    }

    @Override
    public void onDispose() {
        config = null;
    }

    @Override
    public void beforeReload() {
        // nothing
    }

    @Override
    public void onReload() {
        reload();
    }

    private void reload() {
        this.config = PngWindConfigLoader.load(loader);
        LOGGER.info("PNG-WIND configuration loaded");
    }
}
