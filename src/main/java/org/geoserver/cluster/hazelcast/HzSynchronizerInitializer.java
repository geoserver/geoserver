package org.geoserver.cluster.hazelcast;

import java.util.logging.Logger;

import org.geoserver.cluster.ClusterConfig;
import org.geoserver.cluster.ClusterConfigWatcher;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geotools.util.logging.Logging;

import com.hazelcast.core.HazelcastInstance;

public class HzSynchronizerInitializer implements GeoServerInitializer {
    
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");
    
    HzCluster cluster;
    
    public HzSynchronizerInitializer() {
    }
    
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
        
        HzSynchronizer syncher = null;
        
        String method = config.getSyncMethod();
        if ("event".equalsIgnoreCase(method)) {
            syncher = new EventHzSynchronizer(cluster, geoServer);
        }
        else {
            method = "reload"; 
            syncher = new ReloadHzSynchronizer(cluster, geoServer);
        }
        
        syncher.initialize(configWatcher);
        LOGGER.info("Hazelcast synchronizer method is " + method);
    }
    


}
