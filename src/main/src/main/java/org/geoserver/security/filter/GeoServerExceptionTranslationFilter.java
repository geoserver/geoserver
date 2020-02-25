/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.ExceptionTranslationFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.util.StringUtils;

/**
 * Named Exception translation filter
 *
 * <p>The {@link AuthenticationEntryPoint} is of type {@link DynamicAuthenticationEntryPoint}
 *
 * <p>if {@link ExceptionTranslationFilterConfig#getAuthenticationEntryPointName()} is not empty,
 * use this name for a lookup of an authentication filter and use the entry point of this filter.
 *
 * <p>if the name is empty, use {@link GeoServerSecurityFilter#AUTHENTICATION_ENTRY_POINT_HEADER} as
 * a servlet attribute name. Previous authentication filter should put an entry point in this
 * attribute.
 *
 * <p>if still no entry point was a found, use {@link Http403ForbiddenEntryPoint} as a default.
 *
 * @author mcr
 */
public class GeoServerExceptionTranslationFilter extends GeoServerCompositeFilter {

    public static class DynamicAuthenticationEntryPoint implements AuthenticationEntryPoint {

        protected AuthenticationEntryPoint defaultEntryPoint = new Http403ForbiddenEntryPoint();
        protected AuthenticationEntryPoint entryEntryPoint = null;

        public AuthenticationEntryPoint getEntryEntryPoint() {
            return entryEntryPoint;
        }

        public void setEntryEntryPoint(AuthenticationEntryPoint entryEntryPoint) {
            this.entryEntryPoint = entryEntryPoint;
        }

        @Override
        public void commence(
                HttpServletRequest request,
                HttpServletResponse response,
                AuthenticationException authException)
                throws IOException, ServletException {

            AuthenticationEntryPoint aep =
                    (AuthenticationEntryPoint)
                            request.getAttribute(
                                    GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER);
            if (aep != null) // remove from request
            request.removeAttribute(AUTHENTICATION_ENTRY_POINT_HEADER);

            // entry point specified ?
            if (getEntryEntryPoint() != null) {
                getEntryEntryPoint().commence(request, response, authException);
                return;
            }

            // entry point from request ?
            if (aep != null) {
                aep.commence(request, response, authException);
                return;
            }

            // 403, FORBIDDEN
            defaultEntryPoint.commence(request, response, authException);
        }
    };

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        ExceptionTranslationFilterConfig authConfig = (ExceptionTranslationFilterConfig) config;

        DynamicAuthenticationEntryPoint ep = new DynamicAuthenticationEntryPoint();

        if (StringUtils.hasLength(authConfig.getAuthenticationFilterName())) {
            GeoServerSecurityFilter authFilter =
                    getSecurityManager().loadFilter(authConfig.getAuthenticationFilterName());
            ep.setEntryEntryPoint(authFilter.getAuthenticationEntryPoint());
        }

        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        cache.setCreateSessionAllowed(false);
        ExceptionTranslationFilter filter = new ExceptionTranslationFilter(ep, cache);

        AccessDeniedHandlerImpl accessDeniedHandler = new AccessDeniedHandlerImpl();

        if (StringUtils.hasLength(authConfig.getAccessDeniedErrorPage())) {
            // check if page exists
            if (GeoServerExtensions.file(authConfig.getAccessDeniedErrorPage()) != null)
                accessDeniedHandler.setErrorPage(authConfig.getAccessDeniedErrorPage());
            else LOGGER.warning("Cannot find: " + authConfig.getAccessDeniedErrorPage());
        }

        filter.setAccessDeniedHandler(accessDeniedHandler);

        filter.afterPropertiesSet();
        getNestedFilters().add(filter);
    }
}
