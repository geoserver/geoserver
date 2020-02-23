/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

/**
 * Extension point interface for initializing based on configuration.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface GeoServerInitializer {

    /** Performs initialization of GeoServer configuration. */
    void initialize(GeoServer geoServer) throws Exception;
}
