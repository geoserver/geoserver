/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;


/**
 * Extension point interface for initializing based on configuration.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public interface GeoServerInitializer {

    void initialize( GeoServer geoServer ) throws Exception;
}
