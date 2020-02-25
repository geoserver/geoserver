/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.configuration;

import java.io.IOException;

/**
 * class to store and load configuration for {@link ReadOnlyGeoServerLoader}
 *
 * @author carlo cancellieri - GeoSolutions SAS
 */
public final class ReadOnlyConfiguration implements JMSConfigurationExt {

    public static final String READ_ONLY_KEY = "readOnly";

    public static final String DEFAULT_READ_ONLY_VALUE =
            ReadOnlyConfigurationStatus.disabled.toString();

    public static enum ReadOnlyConfigurationStatus {
        enabled,
        disabled;
    }

    @Override
    public void initDefaults(JMSConfiguration config) throws IOException {
        config.putConfiguration(READ_ONLY_KEY, DEFAULT_READ_ONLY_VALUE);
    }

    @Override
    public boolean override(JMSConfiguration config) throws IOException {
        return config.override(READ_ONLY_KEY, DEFAULT_READ_ONLY_VALUE);
    }

    public static boolean isReadOnly(JMSConfiguration config) {
        Object statusObj = config.getConfiguration(ReadOnlyConfiguration.READ_ONLY_KEY);
        if (statusObj == null) {
            statusObj = ReadOnlyConfiguration.DEFAULT_READ_ONLY_VALUE;
        }
        return ReadOnlyConfigurationStatus.valueOf(statusObj.toString())
                .equals(ReadOnlyConfigurationStatus.enabled);
    }
}
