/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import org.geoserver.security.config.SecurityNamedServiceConfig;

/**
 * Common interface for {@link GeoServerRoleService} and {@link GeoServerUserGroupService}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface GeoServerSecurityService {

    /** Initialize from configuration object */
    void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException;

    /** Flag specifying whether the service can create an associated store. */
    boolean canCreateStore();

    /** The name of this service */
    String getName();

    /** Sets the name of this service */
    void setName(String name);

    /**
     * Sets the reference to the security manager facade for the service.
     *
     * <p>This method is called when the service is loaded.
     */
    void setSecurityManager(GeoServerSecurityManager securityManager);

    /**
     * Returns the reference to the security manager, set by {@link
     * #setSecurityManager(GeoServerSecurityManager)}.
     */
    GeoServerSecurityManager getSecurityManager();

    //    /**
    //     * The user details service.
    //     */
    //    GeoserverUserDetailsService getUserDetailsService();
    //
    //    /**
    //     * Sets the user details service.
    //     */
    //    void setUserDetailsService(GeoserverUserDetailsService userDetailsService);
}
