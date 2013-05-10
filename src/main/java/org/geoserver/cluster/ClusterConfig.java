package org.geoserver.cluster;

import java.util.Properties;

public class ClusterConfig extends Properties {

    public boolean isEnabled() {
        return Boolean.valueOf(getProperty("enabled", "true"));
    }

    public String getSyncMethod() {
        return getProperty("sync_method", "reload");
    }

    public int getSyncDelay() {
        return Integer.parseInt(getProperty("sync_delay", "5"));
    }
}
