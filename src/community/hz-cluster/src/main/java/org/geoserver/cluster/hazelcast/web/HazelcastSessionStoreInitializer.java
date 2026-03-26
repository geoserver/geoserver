/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast.web;

import org.apache.wicket.DefaultPageManagerProvider;
import org.apache.wicket.pageStore.IPageStore;
import org.apache.wicket.pageStore.InSessionPageStore;
import org.apache.wicket.settings.StoreSettings;
import org.geoserver.cluster.hazelcast.HzCluster;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerApplicationInitializer;

/**
 * Switches the Wicket page store to session storage, so that Hazelcast session storage can propagate it. The switch is
 * performed on startup if clustering is enabled and session sharing is enabled, otherwise the default disk based page
 * store is used.
 */
public class HazelcastSessionStoreInitializer implements GeoServerApplicationInitializer {

    private final HzCluster cluster;

    public HazelcastSessionStoreInitializer(HzCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public void init(GeoServerApplication application) {
        if (cluster.isEnabled() && cluster.isSessionSharing()) {
            // Replace the page manager with one backed by HttpSessionStore
            application.setPageManagerProvider(new DefaultPageManagerProvider(application) {
                @Override
                protected IPageStore newPersistentStore() {
                    StoreSettings storeSettings = application.getStoreSettings();
                    return new InSessionPageStore(storeSettings.getMaxSizePerSession());
                }
            });
        }
    }
}
