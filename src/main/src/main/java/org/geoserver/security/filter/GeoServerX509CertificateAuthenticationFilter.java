/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.X509CertificateAuthenticationFilterConfig;
import org.geoserver.security.config.X509CertificateAuthenticationFilterConfig.RoleSource;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.RoleCalculator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.authentication.preauth.x509.X509PrincipalExtractor;

/**
 * X509 Authentication Filter
 * 
 * @author mcr
 *
 */
public class GeoServerX509CertificateAuthenticationFilter extends GeoServerPreAuthenticationFilter {
    
    private X509PrincipalExtractor principalExtractor;
    private RoleSource roleSource;
    private String userGroupServiceName;
    private String roleServiceName;

    public RoleSource getRoleSource() {
        return roleSource;
    }

    public void setRoleSource(RoleSource roleSource) {
        this.roleSource = roleSource;
    }

    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    
    public String getRoleServiceName() {
        return roleServiceName;
    }

    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
                        
        X509CertificateAuthenticationFilterConfig authConfig = 
                (X509CertificateAuthenticationFilterConfig) config;

        roleSource=authConfig.getRoleSource();
        userGroupServiceName=authConfig.getUserGroupServiceName();
        roleServiceName=authConfig.getRoleServiceName();
        setPrincipalExtractor(new SubjectDnX509PrincipalExtractor());
        
    }

    @Override
    protected String getPreAuthenticatedPrincipal(HttpServletRequest request) {
        
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) 
            return null;
        
        X509Certificate cert = certs[0];
        String principal= (String) principalExtractor.extractPrincipal(cert);
        if (principal!=null && principal.trim().length()==0)
            principal=null;
        
        try {
            if (principal!=null && RoleSource.UserGroupService.equals(getRoleSource())) {
                GeoServerUserGroupService service = getSecurityManager().loadUserGroupService(getUserGroupServiceName());
                GeoServerUser u = service.getUserByUsername(principal);
                if (u!=null && u.isEnabled()==false)
                    principal=null;            
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return principal;    
    }

    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal) throws IOException{

        if (RoleSource.RoleService.equals(getRoleSource())) 
            return getRolesFromRoleService(request, principal);
        if (RoleSource.UserGroupService.equals(getRoleSource())) 
            return getRolesFromUserGroupService(request, principal);
        
        throw new RuntimeException("Never should reach this point");

    }
    
    /**
     * Calculates roles from a {@link GeoServerRoleService}
     * The default service is {@link GeoServerSecurityManager#getActiveRoleService()}
     * 
     * The result contains all inherited roles, but no personalized roles
     * 
     * @param request
     * @param principal
     * @return
     * @throws IOException
     */
    protected Collection<GeoServerRole> getRolesFromRoleService(HttpServletRequest request, String principal) throws IOException{
        Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();
        boolean useActiveService = getRoleServiceName()==null || 
                getRoleServiceName().trim().length()==0;
      
        GeoServerRoleService service = useActiveService ?
              getSecurityManager().getActiveRoleService() :
              getSecurityManager().loadRoleService(getRoleServiceName());

        roles.addAll(service.getRolesForUser(principal));       
      
        RoleCalculator calc = new RoleCalculator(service);
        calc.addInheritedRoles(roles);
        return roles;        
    }
    
    /**
     * Calculates roles using a {@link GeoServerUserGroupService}
     * if the principal is not found, an empty collection is returned
     * 
     * @param request
     * @param principal
     * @return
     * @throws IOException
     */
    protected Collection<GeoServerRole> getRolesFromUserGroupService(HttpServletRequest request, String principal) throws IOException{
        Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();
        
        GeoServerUserGroupService service = getSecurityManager().loadUserGroupService(getUserGroupServiceName());
        UserDetails details=null;
        try {
             details = service.loadUserByUsername(principal);
        } catch (UsernameNotFoundException ex) {
            LOGGER.log(Level.WARNING,"User "+ principal + " not found in " + getUserGroupServiceName());
        }
        
        if (details!=null) {
            for (GrantedAuthority auth : details.getAuthorities())
                roles.add((GeoServerRole)auth);
        }
        return roles;        
    }

    public X509PrincipalExtractor getPrincipalExtractor() {
        return principalExtractor;
    }

    public void setPrincipalExtractor(X509PrincipalExtractor principalExtractor) {
        this.principalExtractor = principalExtractor;
    }
    



}
