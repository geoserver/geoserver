/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.configuration;

import java.io.IOException;

/**
 * class to store and load configuration from global var or properties file
 *
 * @author carlo cancellieri - GeoSolutions SAS
 */
public final class BrokerConfiguration implements JMSConfigurationExt {

    public static final String BROKER_URL_KEY = "brokerURL";

    public static final String DEFAULT_BROKER_URL = "";

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        config.putConfiguration(BROKER_URL_KEY, DEFAULT_BROKER_URL);
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        return config.override(BROKER_URL_KEY, DEFAULT_BROKER_URL);
    }
}
