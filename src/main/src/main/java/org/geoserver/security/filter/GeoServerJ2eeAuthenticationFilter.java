/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.config.J2eeAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.J2eeAuthenticationFilterConfig.RolesTakenFrom;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;

/**
 * J2EE Authentication Filter
 * 
 * @author mcr
 *
 */
public class GeoServerJ2eeAuthenticationFilter extends GeoServerPreAuthenticationFilter {
    
    private  String roleServiceName;
    
    private RolesTakenFrom rolesTakenFrom = RolesTakenFrom.J2EE;
    
    
    
    public String getRoleServiceName() {
        return roleServiceName;
    }

    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }
    
    public RolesTakenFrom getRolesTakenFrom() {
        return rolesTakenFrom;
    }

    public void setRolesTakenFrom(RolesTakenFrom rolesTakenFrom) {
        this.rolesTakenFrom = rolesTakenFrom;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
                        
        J2eeAuthenticationFilterConfig authConfig = 
                (J2eeAuthenticationFilterConfig) config;
        
        roleServiceName=authConfig.getRoleServiceName();
        rolesTakenFrom = authConfig.getRolesTakenFrom();
    }

    @Override
    protected String getPreAuthenticatedPrincipal(HttpServletRequest request) {
        return request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();
    }

    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal) throws IOException{
        Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();
        boolean useActiveService = getRoleServiceName()==null || 
                getRoleServiceName().trim().length()==0;
      
        GeoServerRoleService service = useActiveService ?
              getSecurityManager().getActiveRoleService() :
              getSecurityManager().loadRoleService(getRoleServiceName());
        
        if(rolesTakenFrom == RolesTakenFrom.J2EE || rolesTakenFrom == RolesTakenFrom.Both) {
            for (GeoServerRole role: service.getRoles())
              if (request.isUserInRole(role.getAuthority()))
                  roles.add(role);
        }
        
        if((rolesTakenFrom == RolesTakenFrom.RoleService || rolesTakenFrom == RolesTakenFrom.Both) && service != null) {
            String username = getPreAuthenticatedPrincipal(request);
            if(username != null) {
                roles.addAll(service.getRolesForUser(username));
            }
        }
      
        RoleCalculator calc = new RoleCalculator(service);
        calc.addInheritedRoles(roles);
        calc.addMappedSystemRoles(roles);
        return roles;        
    }
    
    
}
