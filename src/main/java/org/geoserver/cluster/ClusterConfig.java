package org.geoserver.cluster;

import java.util.Properties;

/**
 * Configuration properties for clustered configuration/catalog.
 *
 */
public class ClusterConfig extends Properties {

    /**
     * @return is clustering enabled
     */
    public boolean isEnabled() {
        return Boolean.valueOf(getProperty("enabled", "true"));
    }

    /**
     * @return what method should be used for synchronization
     */
    public String getSyncMethod() {
        return getProperty("sync_method", "reload");
    }

    /**
     * @return how long to wait and accumulate changes before synchronizing
     */
    public int getSyncDelay() {
        return Integer.parseInt(getProperty("sync_delay", "5"));
    }
}
