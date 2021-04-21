/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.platform.ContextLoadedEvent;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationListener;

/**
 * Forces reload of the templates on big configuration changes.
 *
 * <p>To be considered: reload also when the OSEOInfo is modified (could be pointing at a different
 * database) or when the OpenSearchAccess is modified, or when the datastore that
 * JDBCOpenSearchAccess is modified.
 */
public class TemplatesReloader
        implements GeoServerLifecycleHandler, ApplicationListener<ContextLoadedEvent> {
    static final Logger LOGGER = Logging.getLogger(TemplatesReloader.class);

    AbstractTemplates templates;

    public TemplatesReloader(AbstractTemplates templates) {
        this.templates = templates;
    }

    @Override
    public void onReset() {
        reload();
    }

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {}

    @Override
    public void onReload() {
        reload();
    }

    @Override
    public void onApplicationEvent(ContextLoadedEvent contextStartedEvent) {
        reload();
    }

    private void reload() {
        try {
            this.templates.reloadTemplates();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to reload templates", e);
        }
    }
}
