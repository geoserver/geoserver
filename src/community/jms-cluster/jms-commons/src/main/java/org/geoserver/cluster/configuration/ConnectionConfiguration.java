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
public final class ConnectionConfiguration implements JMSConfigurationExt {

    public static final String CONNECTION_KEY = "connection";

    // times to test (connection)
    public static final String CONNECTION_RETRY_KEY = "connection.retry";
    public static final Integer DEFAULT_CONNECTION_RETRY = 10;

    // millisecs to wait between tests (connection)
    public static final String CONNECTION_MAXWAIT_KEY = "connection.maxwait";
    public static final Long DEFAULT_CONNECTION_MAXWAIT = 500L;

    public static final ConnectionConfigurationStatus DEFAULT_CONNECTION_STATUS =
            ConnectionConfigurationStatus.enabled;

    public static enum ConnectionConfigurationStatus {
        enabled,
        disabled
    }

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        config.putConfiguration(CONNECTION_KEY, DEFAULT_CONNECTION_STATUS.toString());
        config.putConfiguration(CONNECTION_RETRY_KEY, DEFAULT_CONNECTION_RETRY.toString());
        config.putConfiguration(CONNECTION_MAXWAIT_KEY, DEFAULT_CONNECTION_MAXWAIT.toString());
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        boolean override = config.override(CONNECTION_KEY, DEFAULT_CONNECTION_STATUS);
        override |= config.override(CONNECTION_RETRY_KEY, DEFAULT_CONNECTION_RETRY);
        override |= config.override(CONNECTION_MAXWAIT_KEY, DEFAULT_CONNECTION_MAXWAIT);
        return override;
    }
}
