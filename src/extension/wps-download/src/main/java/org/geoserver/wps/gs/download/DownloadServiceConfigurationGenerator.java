/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.io.IOException;

/**
 * Simple interface to get the {@link DownloadServiceConfiguration}.
 *
 * @author Simone Giannecchini, GeoSolutions
 */
public interface DownloadServiceConfigurationGenerator {

    /** @return the {@link DownloadServiceConfiguration} object */
    DownloadServiceConfiguration getConfiguration();

    default void setConfiguration(DownloadServiceConfiguration configuration) throws IOException {
        throw new UnsupportedOperationException("The download configuration is read only");
    }
}
