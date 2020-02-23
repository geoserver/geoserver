/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.hz;

import static org.geoserver.wps.hz.HazelcastStatusStore.*;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;

/**
 * This class loads the Hazelcast configuration, making sure it contains the expected elements
 *
 * @author Andrea Aime - GeoSolutions
 */
public class HazelcastLoader implements DisposableBean {
    private static final Logger LOGGER = Logging.getLogger(HazelcastLoader.class);

    /** Name of the Hazelcast XML file to use */
    public static final String HAZELCAST_NAME = "hazelcast.xml";

    /** Hazelcast instance to pass to the {@link HazelcastCacheProvider} class */
    private HazelcastInstance instance;

    /** Loads a new {@link HazelcastInstance} from the data directory, and */
    public HazelcastLoader(ResourceStore store) throws IOException {
        // see if we have the hazelcast configuration ready, otherwise create one from the classpath
        Resource resource = store.get(HAZELCAST_NAME);
        if (resource.getType() == Type.UNDEFINED) {
            try (OutputStream os = resource.out();
                    InputStream is = HazelcastLoader.class.getResourceAsStream(HAZELCAST_NAME)) {
                IOUtils.copy(is, os);
            }
        }
        try (InputStream is = resource.in()) {
            Config config = new XmlConfigBuilder(is).build();
            validateConfiguration(config);
            instance = Hazelcast.newHazelcastInstance(config);
        }
    }

    /** Starts with a given Hazelcast instance. */
    HazelcastLoader(HazelcastInstance instance) {
        this.instance = instance;
        this.validateConfiguration(instance.getConfig());
    }

    /**
     * Returns the Hazelcast instance to use
     *
     * @return Hazelcast instance if present or null
     */
    public HazelcastInstance getInstance() {
        return instance;
    }

    /**
     * Validation for an input {@link Config} object provided. This method ensures that the input
     * configuration contains the right map, and issues warning in case the map is configured as a
     * cache instead of as a regular distributed map
     */
    private void validateConfiguration(Config config) {
        LOGGER.fine("Checking configuration");
        if (config == null) {
            throw new IllegalArgumentException("Hazelcast configuration should not be null");
        }
        // Check if the cache map is present
        if (!config.getMapConfigs().containsKey(EXECUTION_STATUS_MAP)) {
            throw new IllegalArgumentException(
                    "Hazelcast configuration is missing the status map, should be called: "
                            + EXECUTION_STATUS_MAP);
        }

        // make some sanity checks on the map
        MapConfig mapConfig = config.getMapConfig(EXECUTION_STATUS_MAP);
        // Check size policy
        if (mapConfig.getMaxSizeConfig().getSize() > 0) {
            LOGGER.warning(
                    "The WPS status map "
                            + EXECUTION_STATUS_MAP
                            + " has a max size set, it should be unbounded so that no status is lost"
                            + " before the configured timeout");
        }
        if (mapConfig.getEvictionPolicy() != MapConfig.DEFAULT_EVICTION_POLICY) {
            LOGGER.warning(
                    "The WPS status map "
                            + EXECUTION_STATUS_MAP
                            + " has a eviction policy set, it should not automatically evict entries so that "
                            + " no status is lost before the configured timeout");
        }
    }

    @Override
    public void destroy() throws Exception {
        instance.shutdown();
    }
}
