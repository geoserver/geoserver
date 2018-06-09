/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import javax.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;

/**
 * Configuration for security context persistence
 *
 * <p>if {@link #allowSessionCreation} is <code>true</code>, creation of a {@link HttpSession}
 * object is allowed and an {@link Authentication} object can be stored to avoid re-authentication
 * fore each request
 *
 * <p>Should be <code>false</code> for stateless services
 *
 * @author mcr
 */
public class SecurityContextPersistenceFilterConfig extends SecurityFilterConfig {

    private static final long serialVersionUID = 1L;
    private boolean allowSessionCreation;

    public boolean isAllowSessionCreation() {
        return allowSessionCreation;
    }

    public void setAllowSessionCreation(boolean allowSessionCreation) {
        this.allowSessionCreation = allowSessionCreation;
    }
}
