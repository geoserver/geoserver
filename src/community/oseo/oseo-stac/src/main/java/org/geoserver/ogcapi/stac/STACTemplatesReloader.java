/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.springframework.stereotype.Component;

/**
 * Forces reload of the templates on big configuration changes.
 *
 * <p>To be considered: reload also when the OSEOInfo is modified (could be pointing at a different
 * database) or when the OpenSearchAccess is modified, or when the datastore that
 * JDBCOpenSearchAccess is modified.
 */
@Component
public class STACTemplatesReloader implements GeoServerLifecycleHandler {
    STACTemplates templates;

    public STACTemplatesReloader(STACTemplates templates) {
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
