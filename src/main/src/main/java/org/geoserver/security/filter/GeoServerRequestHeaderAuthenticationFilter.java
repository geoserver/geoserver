/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.config.RequestHeaderAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * Request header Authentication Filter
 *
 * @author mcr
 */
public class GeoServerRequestHeaderAuthenticationFilter extends GeoServerPreAuthenticatedUserNameFilter {

    private String principalHeaderAttribute;

    public String getPrincipalHeaderAttribute() {
        return principalHeaderAttribute;
    }

    public void setPrincipalHeaderAttribute(String principalHeaderAttribute) {
        this.principalHeaderAttribute = principalHeaderAttribute;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        RequestHeaderAuthenticationFilterConfig authConfig = (RequestHeaderAuthenticationFilterConfig) config;
        setPrincipalHeaderAttribute(authConfig.getPrincipalHeaderAttribute());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String principalName = getPreAuthenticatedPrincipalName((HttpServletRequest) request);
        Authentication preAuth = SecurityContextHolder.getContext().getAuthentication();
        // If a pre-auth token exists but the request has no principal name anymore
        // or differs from the one being sent in the headers, clear the
        // security context, or else the user will remain authenticated.
        if (preAuth instanceof PreAuthenticatedAuthenticationToken
                && ((null == principalName)
                        || (!principalName.equals(preAuth.getPrincipal().toString())))) {
            SecurityContextHolder.clearContext();
        }
        super.doFilter(request, response, chain);
    }

    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        return request.getHeader(getPrincipalHeaderAttribute());
    }
}
