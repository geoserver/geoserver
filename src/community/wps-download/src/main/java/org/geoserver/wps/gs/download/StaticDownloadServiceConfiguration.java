/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

/**
 * Implementation of {@link DownloadServiceConfigurationGenerator} that uses a specific {@link
 * DownloadServiceConfiguration}. This configuraton cannot be changed like for the {@link
 * DownloadServiceConfigurationWatcher} class.
 *
 * @author Simone Giannecchini, GeoSolutions
 */
public class StaticDownloadServiceConfiguration implements DownloadServiceConfigurationGenerator {

    /** The {@link DownloadServiceConfiguration} instance contained. */
    DownloadServiceConfiguration config;

    /**
     * Constructor
     *
     * <p>This constructor takes an external {@link DownloadServiceConfiguration} object and stores
     * it without modifications.
     */
    public StaticDownloadServiceConfiguration(DownloadServiceConfiguration config) {
        this.config = config;
    }

    /**
     * Constructor.
     *
     * <p>It creates a new {@link DownloadServiceConfiguration} object with default values
     */
    public StaticDownloadServiceConfiguration() {
        config = new DownloadServiceConfiguration();
    }

    @Override
    public DownloadServiceConfiguration getConfiguration() {
        return config;
    }
}
