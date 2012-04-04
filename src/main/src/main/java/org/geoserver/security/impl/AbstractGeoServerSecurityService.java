/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Common base class for user group and role services.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class AbstractGeoServerSecurityService implements GeoServerSecurityService {

    public static String DEFAULT_NAME = "default";

    /** logger */
    protected static Logger LOGGER = 
        org.geotools.util.logging.Logging.getLogger("org.geoserver.security");

    protected String name;
    protected GeoServerSecurityManager securityManager;

    protected AbstractGeoServerSecurityService() {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public GeoServerSecurityManager getSecurityManager() {
        return securityManager;
    }

    @Override
    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }
    
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        if (config!=null)
            this.name=config.getName();
    }

    @Override
    public boolean canCreateStore() {
        return false;
    }

    /**
     * Authentication filters with an {@link AuthenticationEntryPoint} must
     * return their entry point 
     * 
     * @return
     */
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return null;
    }
}
