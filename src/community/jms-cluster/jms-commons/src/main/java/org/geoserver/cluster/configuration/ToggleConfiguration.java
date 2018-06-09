/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.configuration;

/**
 * Defines the configuration parameters and defaults for the Master and the Slave toggles.
 *
 * @author carlo cancellieri - GeoSolutions SAS
 */
public final class ToggleConfiguration implements JMSConfigurationExt {

    public static final String TOGGLE_MASTER_KEY = "toggleMaster";

    // the master is disabled by default
    public static final String DEFAULT_MASTER_STATUS = "true";

    public static final String TOGGLE_SLAVE_KEY = "toggleSlave";

    // the slave is disabled by default
    public static final String DEFAULT_SLAVE_STATUS = "true";

    @Override
    public void initDefaults(JMSConfiguration config) {
        config.putConfiguration(TOGGLE_MASTER_KEY, DEFAULT_MASTER_STATUS);
        config.putConfiguration(TOGGLE_SLAVE_KEY, DEFAULT_SLAVE_STATUS);
    }

    @Override
    public boolean override(JMSConfiguration config) {
        return config.override(TOGGLE_MASTER_KEY, DEFAULT_MASTER_STATUS)
                || config.override(TOGGLE_SLAVE_KEY, DEFAULT_SLAVE_STATUS);
    }
}
