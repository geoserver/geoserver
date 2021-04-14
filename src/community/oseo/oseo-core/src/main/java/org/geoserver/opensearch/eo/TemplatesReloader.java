/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import org.geoserver.config.impl.GeoServerLifecycleHandler;

/**
 * Forces reload of the templates on big configuration changes.
 *
 * <p>To be considered: reload also when the OSEOInfo is modified (could be pointing at a different
 * database) or when the OpenSearchAccess is modified, or when the datastore that
 * JDBCOpenSearchAccess is modified.
 */
public class TemplatesReloader implements GeoServerLifecycleHandler {
    AbstractTemplates templates;

    public TemplatesReloader(AbstractTemplates templates) {
        this.templates = templates;
    }

    @Override
    public void onReset() {
        this.templates.reloadTemplates();
    }

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {}

    @Override
    public void onReload() {
        this.templates.reloadTemplates();
    }
}
