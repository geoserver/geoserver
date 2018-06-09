/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import java.util.Properties;

/** Configuration properties for clustered configuration/catalog. */
public class ClusterConfig extends Properties {

    private static final long serialVersionUID = 1L;

    /** @return is clustering enabled */
    public boolean isEnabled() {
        return Boolean.valueOf(getProperty("enabled", "true"));
    }

    /** @return what method should be used for synchronization */
    public String getSyncMethod() {
        return getProperty("sync_method", "reload");
    }

    /** @return how long to wait and accumulate changes before synchronizing */
    public int getSyncDelay() {
        return Integer.parseInt(getProperty("sync_delay", "5"));
    }

    /**
     * @return milliseconds to wait for node ack notifications upon sending a config change event.
     *     Defaults to 2000ms.
     */
    public int getAckTimeoutMillis() {
        return Integer.parseInt(getProperty("acktimeout", "2000"));
    }
}
