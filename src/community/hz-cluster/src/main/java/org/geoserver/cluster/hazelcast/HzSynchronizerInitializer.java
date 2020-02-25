/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.cluster.ClusterConfig;
import org.geoserver.cluster.ClusterConfigWatcher;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.platform.ContextLoadedEvent;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

public class HzSynchronizerInitializer
        implements GeoServerInitializer, ApplicationListener<ApplicationEvent> {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");

    HzCluster cluster;

    HzSynchronizer syncher = null;

    public HzSynchronizerInitializer() {}

    public void setCluster(HzCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        ClusterConfigWatcher configWatcher = cluster.getConfigWatcher();
        ClusterConfig config = configWatcher.get();

        if (!config.isEnabled()) {
            LOGGER.info("Hazelcast synchronization disabled");
            return;
        }

        @SuppressWarnings("unused")
        HazelcastInstance hz = cluster.getHz();

        String method = config.getSyncMethod();
        if ("event".equalsIgnoreCase(method)) {
            syncher = new EventHzSynchronizer(cluster, geoServer);
        } else {
            method = "reload";
            syncher = new ReloadHzSynchronizer(cluster, geoServer);
        }
        syncher.initialize(configWatcher);
        LOGGER.info("Hazelcast synchronizer method is " + method);
    }

    /**
     * Enables processing of config change events when a {@link ContextLoadedEvent} is received, and
     * disables it when a {@link ContextClosedEvent} is received, in order to avoid the hazelcast
     * synchronizer to try to dispatch events to an stale {@link Catalog} or one that's in an
     * inconsistent state.
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (syncher == null) {
            return;
        }
        if (event instanceof ContextLoadedEvent) {
            syncher.start();
        } else if (event instanceof ContextClosedEvent) {
            syncher.stop();
        }
    }
}
