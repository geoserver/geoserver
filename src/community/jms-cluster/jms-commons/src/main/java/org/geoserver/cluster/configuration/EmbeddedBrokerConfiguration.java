/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.configuration;

import java.io.IOException;
import org.geoserver.cluster.JMSFactory;

/**
 * class to store and load configuration for {@link JMSFactory}
 *
 * @author carlo cancellieri - GeoSolutions SAS
 */
public class EmbeddedBrokerConfiguration implements JMSConfigurationExt {

    public static final String EMBEDDED_BROKER_KEY = "embeddedBroker";

    public static final String DEFAULT_EMBEDDED_BROKER_VALUE =
            ConfigurationStatus.enabled.toString();

    public static final String EMBEDDED_BROKER_PROPERTIES_KEY = "embeddedBrokerProperties";

    public static final String DEFAULT_EMBEDDED_BROKER_PROPERTIES_VALUE =
            "embedded-broker.properties";

    public static enum ConfigurationStatus {
        enabled,
        disabled;
    }

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        config.putConfiguration(EMBEDDED_BROKER_KEY, DEFAULT_EMBEDDED_BROKER_VALUE);
        config.putConfiguration(
                EMBEDDED_BROKER_PROPERTIES_KEY, DEFAULT_EMBEDDED_BROKER_PROPERTIES_VALUE);
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        return config.override(EMBEDDED_BROKER_KEY, DEFAULT_EMBEDDED_BROKER_VALUE)
                || config.override(
                        EMBEDDED_BROKER_PROPERTIES_KEY, DEFAULT_EMBEDDED_BROKER_PROPERTIES_VALUE);
    }

    public static boolean isEnabled(JMSConfiguration config) {
        Object statusObj = config.getConfiguration(EMBEDDED_BROKER_KEY);
        if (statusObj == null) {
            statusObj = DEFAULT_EMBEDDED_BROKER_VALUE;
        }
        return ConfigurationStatus.valueOf(statusObj.toString())
                .equals(ConfigurationStatus.enabled);
    }
}
