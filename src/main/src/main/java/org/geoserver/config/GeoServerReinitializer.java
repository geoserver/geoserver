/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

/**
 * Extension point interface for initializing based on configuration. Intended to be run both upon
 * configuration initialization, and upon reload
 */
public interface GeoServerReinitializer extends GeoServerInitializer {

    /**
     * Performs initialization of GeoServer configuration, as well as any actions that should be
     * performed only when reloading the configuration.
     */
    default void reinitialize(GeoServer geoServer) throws Exception {
        initialize(geoServer);
    };
}
