/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;

/**
 * Basic implementation for filters supporting J2EE as a {@link
 * org.geoserver.security.config.RoleSource}
 *
 * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
 */
public abstract class GeoServerJ2eeBaseAuthenticationFilter
        extends GeoServerPreAuthenticatedUserNameFilter {

    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal)
            throws IOException {
        if (J2eeAuthenticationBaseFilterConfig.J2EERoleSource.J2EE.equals(getRoleSource())) {
            return getRolesFromJ2EE(request, principal);
        }
        return super.getRoles(request, principal);
    }

    /** Implements roles retrieval from the J2EE container. */
    protected Collection<GeoServerRole> getRolesFromJ2EE(
            HttpServletRequest request, String principal) throws IOException {

        Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();
        boolean useActiveService =
                getRoleServiceName() == null || getRoleServiceName().trim().length() == 0;

        GeoServerRoleService service =
                useActiveService
                        ? getSecurityManager().getActiveRoleService()
                        : getSecurityManager().loadRoleService(getRoleServiceName());

        for (GeoServerRole role : service.getRoles())
            if (request.isUserInRole(role.getAuthority())) roles.add(role);

        RoleCalculator calc = new RoleCalculator(service);
        calc.addInheritedRoles(roles);
        calc.addMappedSystemRoles(roles);
        return roles;
    }
}
