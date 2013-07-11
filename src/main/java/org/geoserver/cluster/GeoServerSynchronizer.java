package org.geoserver.cluster;

import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.config.ConfigurationListenerAdapter;

/**
 * Base class for synchronising catalog changes across a cluster.
 *
 */
public class GeoServerSynchronizer extends ConfigurationListenerAdapter 
    implements CatalogListener {

    protected ClusterConfigWatcher configWatcher;

    public final void initialize(ClusterConfigWatcher configWatcher) {
        this.configWatcher = configWatcher;
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
    }

}
