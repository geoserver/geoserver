/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.Collection;

import org.geoserver.security.impl.GeoServerRole;
import org.springframework.security.core.GrantedAuthority;

/**
 * Interface for {@link GeoServerRole} converters
 * 
 * @author mcr
 *
 */
public interface GeoServerRoleConverter {

    /**
     * Converts a {@link GeoServerRole} to a string 
     * The {@link GeoServerRole#getUserName()} is not included
     * 
     * @param role
     * @return
     */
    public abstract String convertRoleToString(GeoServerRole role);

    /**
     * converts a collection of roles to a string
     * 
     * @param roles
     * @return
     */
    public abstract String convertRolesToString(Collection<? extends GrantedAuthority> roles);

    /**
     * creates a collection of roles from a string
     * 
     * The userName may be null
     * 
     * @param rolesString
     * @param userName
     * @return
     */
    public abstract Collection<GeoServerRole> convertRolesFromString(String rolesString,
            String userName);

    /**
     * Creates a {@link GeoServerRole} from a string
     * May return <code>null</code> if rolesString is null or empty
     * 
     * The userName may be null and should only be passed if the role
     * has personalized role parameters
     * 
     * @param roleString
     * @param userName
     * @return 
     */
    public abstract GeoServerRole convertRoleFromString(String roleString, String userName);

}
