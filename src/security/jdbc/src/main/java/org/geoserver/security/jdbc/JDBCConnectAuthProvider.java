/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Authentication Provider based on a successful JDBC Connect
 * 
 * 
 * @author christian
 *
 */
public class JDBCConnectAuthProvider extends GeoServerAuthenticationProvider {

    protected String connectUrl,driverClassName,userGroupServiceName;
    
    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {
        
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) authentication;
        
        // check for valid user name
        if (token.getPrincipal()==null || token.getPrincipal().toString().isEmpty()) 
            return null;
        String user = token.getPrincipal().toString();
        String password = token.getCredentials()==null ? "" : token.getCredentials().toString();
        

        UserDetails details = null;
        
        if (userGroupServiceName!=null) {
            try {
                GeoServerUserGroupService service = getSecurityManager().loadUserGroupService(userGroupServiceName);
                details = service.loadUserByUsername(user);
                if (details.isEnabled()==false) return null; // user is disabled
            } catch (IOException ex ) {
                throw new AuthenticationServiceException(ex.getLocalizedMessage(),ex);                
            }
        }
                        
        Connection con = null;
        try {
            con =DriverManager.getConnection(connectUrl, user, password);                        
        } catch (SQLException ex) {
            // nasty situation, it is not clear if the connect URL is invalid or the 
            // credentials are not ok, in any case throw a bad credentials exception
            // TODO, check exception message
            throw new BadCredentialsException(user);
        } finally {
            if (con!=null) {
                try { 
                    con.close(); 
                } catch (SQLException ex2) {
                    // do nothing, give up
                }
            }
                
        }
        UsernamePasswordAuthenticationToken result = null;
        Set<GrantedAuthority> roles = new HashSet<GrantedAuthority>();
        if (details!=null) {            
            roles.addAll(details.getAuthorities());                        
        } else {        
            RoleCalculator calc = new RoleCalculator(getSecurityManager().getActiveRoleService());
            try {
                roles.addAll(calc.calculateRoles(new GeoServerUser(user)));
            } catch (IOException e) {
                throw new AuthenticationServiceException(e.getLocalizedMessage(),e);
            }                        
        }   
        roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        result = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),null,roles);
        result.setDetails(authentication.getDetails());
        return result;                        
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        JDBCConnectAuthProviderConfig jdbcConfig = (JDBCConnectAuthProviderConfig) config;
        userGroupServiceName=jdbcConfig.getUserGroupServiceName();
        connectUrl=jdbcConfig.getConnectURL();
        driverClassName=jdbcConfig.getDriverClassName();
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        
    }

}
