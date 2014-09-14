/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.spring.security;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;
import org.springframework.security.providers.anonymous.AnonymousProcessingFilter;
import org.springframework.security.ui.AuthenticationDetailsSource;
import org.springframework.security.ui.AuthenticationDetailsSourceImpl;
import org.geoserver.xacml.role.XACMLRole;

/**
 * Creating an AnoynmaAuthenticationToken with XACML Roles
 * 
 * @author Christian Mueller
 * 
 */
public class XACMLAnonymousProcessingFilter extends AnonymousProcessingFilter {

    private AuthenticationDetailsSource authenticationDetailsSource = new AuthenticationDetailsSourceImpl();

    @Override
    protected Authentication createAuthentication(HttpServletRequest request) {
        GrantedAuthority[] auths = getUserAttribute().getAuthorities();
        XACMLRole[] roles = new XACMLRole[auths.length];
        for (int i = 0; i < auths.length; i++) {
            roles[i] = new XACMLRole(auths[i].getAuthority());
            roles[i].setRoleAttributesProcessed(true); // No userinfo for anonymous
        }

        AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken(getKey(),
                getUserAttribute().getPassword(), roles);
        auth.setDetails(authenticationDetailsSource.buildDetails((HttpServletRequest) request));
        return auth;
    }

    @Override
    public void setAuthenticationDetailsSource(
            AuthenticationDetailsSource authenticationDetailsSource) {
        super.setAuthenticationDetailsSource(authenticationDetailsSource);
        this.authenticationDetailsSource = authenticationDetailsSource;
    }

}
