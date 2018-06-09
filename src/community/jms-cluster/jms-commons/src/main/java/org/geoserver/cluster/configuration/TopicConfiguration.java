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
public final class TopicConfiguration implements JMSConfigurationExt {

    public static final String TOPIC_NAME_KEY = "topicName";

    public static final String DEFAULT_TOPIC_NAME = "VirtualTopic.geoserver";

    public static final String DURABLE_KEY = "durable";

    public static final String DEFAULT_DURABLE_NAME = "true";

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        String url = null;

        config.putConfiguration(TOPIC_NAME_KEY, url != null ? url : DEFAULT_TOPIC_NAME);

        config.putConfiguration(DURABLE_KEY, url != null ? url : DEFAULT_DURABLE_NAME);
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        return config.override(TOPIC_NAME_KEY, DEFAULT_TOPIC_NAME)
                || config.override(DURABLE_KEY, DEFAULT_DURABLE_NAME);
    }
}
