/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl;

import java.io.IOException;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.JMSConfigurationExt;

/**
 * class to store and load configuration
 *
 * @author carlo cancellieri - GeoSolutions SAS
 */
public final class ActiveMQEmbeddedBrokerConfiguration implements JMSConfigurationExt {

    public static final String BROKER_URL_KEY = "xbeanURL";

    public static final String DEFAULT_BROKER_URL = "./broker.xml";

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        config.putConfiguration(BROKER_URL_KEY, DEFAULT_BROKER_URL);
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        return config.override(BROKER_URL_KEY, DEFAULT_BROKER_URL);
    }
}
