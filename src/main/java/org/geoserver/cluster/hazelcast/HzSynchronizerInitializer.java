package org.geoserver.cluster.hazelcast;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.cluster.ClusterConfig;
import org.geoserver.cluster.ClusterConfigWatcher;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

import com.hazelcast.core.HazelcastInstance;

public class HzSynchronizerInitializer implements GeoServerInitializer {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");

    static final String CONFIG_FILENAME = "cluster.properties";

    /** hazelcast */
    HazelcastInstance hz;

    public HzSynchronizerInitializer(HazelcastInstance hz) {
        this.hz = hz;
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        ClusterConfigWatcher configWatcher = loadConfig(geoServer);
        ClusterConfig config = configWatcher.get();

        if (!config.isEnabled()) {
            LOGGER.info("Hazelcast sychronizer disabled");
            return;
        }

        LOGGER.info("Simple Hazelcast sychronizer active");
        SimpleHzSynchronizer syncher = new SimpleHzSynchronizer(hz, geoServer);
        syncher.initialize(configWatcher);
    }

    ClusterConfigWatcher loadConfig(GeoServer geoServer) throws IOException {
        GeoServerResourceLoader rl = geoServer.getCatalog().getResourceLoader();
        File f = rl.find(CONFIG_FILENAME);
        if (f == null) {
            f = rl.createFile(CONFIG_FILENAME);
        }

        return new ClusterConfigWatcher(f);
    }
}
