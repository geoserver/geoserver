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
import javax.servlet.http.HttpServletResponse;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.config.RoleFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Servlet filter for sending the roles (and role parameters) of the authenticated principal to
 * client
 *
 * @author mcr
 */
public class GeoServerRoleFilter extends GeoServerSecurityFilter {

    protected GeoServerRoleConverter converter;
    protected String headerAttribute;

    public static String DEFAULT_ROLE_CONVERTER = "roleConverter";
    public static String DEFAULT_HEADER_ATTRIBUTE = "roles";

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        RoleFilterConfig roleConfig = (RoleFilterConfig) config;

        headerAttribute = roleConfig.getHttpResponseHeaderAttrForIncludedRoles();
        // TODO, Justin, is this ok ?
        String converterName = roleConfig.getRoleConverterName();
        if (converterName == null || converterName.length() == 0)
            converter = GeoServerExtensions.bean(GeoServerRoleConverter.class);
        else converter = (GeoServerRoleConverter) GeoServerExtensions.bean(converterName);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        chain.doFilter(request, response);

        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            Authentication auth = context.getAuthentication();
            if (auth != null) {
                String roleString = converter.convertRolesToString(auth.getAuthorities());
                ((HttpServletResponse) response).setHeader(headerAttribute, roleString);
            }
        }
    }
}
