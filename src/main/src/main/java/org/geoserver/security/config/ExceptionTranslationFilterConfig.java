/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.filter.GeoServerExceptionTranslationFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * {@link GeoServerExceptionTranslationFilter} configuration object.
 *
 * <p>The property {@link #authenticationFilterName} is the name of an authentication filter
 * providing the {@link AuthenticationEntryPoint} object. The entry point is needed in the case of
 * of a {@link AuthenticationException}.
 *
 * <p>IMPORTANT: if no authentication entry point is given, {@link
 * GeoServerExceptionTranslationFilter} uses the entry point found in the servlet request attribute
 * {@link GeoServerSecurityFilter#AUTHENTICATION_ENTRY_POINT_HEADER}
 *
 * <p>The property {@link #accessDeniedErrorPage} is optional and needed in case of an {@link
 * AccessDeniedException}. Geoserver default is <b>/accessDenied.jsp</b>
 *
 * @author christian
 */
public class ExceptionTranslationFilterConfig extends SecurityFilterConfig {

    private static final long serialVersionUID = 1L;

    private String authenticationFilterName;
    private String accessDeniedErrorPage;

    public String getAccessDeniedErrorPage() {
        return accessDeniedErrorPage;
    }

    public void setAccessDeniedErrorPage(String accessDeniedErrorPage) {
        this.accessDeniedErrorPage = accessDeniedErrorPage;
    }

    public String getAuthenticationFilterName() {
        return authenticationFilterName;
    }

    public void setAuthenticationFilterName(String authenticationFilterName) {
        this.authenticationFilterName = authenticationFilterName;
    }
}
