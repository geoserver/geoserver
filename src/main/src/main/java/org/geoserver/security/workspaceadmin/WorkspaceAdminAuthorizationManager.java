/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import static org.geoserver.security.filter.GeoServerSecurityInterceptorFilter.ACCESS_ABSTAIN;
import static org.geoserver.security.filter.GeoServerSecurityInterceptorFilter.ACCESS_GRANTED;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.function.Supplier;
import org.geoserver.security.filter.GeoServerSecurityInterceptorFilter;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.StringUtils;

/**
 * {@link AuthorizationManager} that grants access when the current user is a workspace administrator and the request
 * matches one of the {@link WorkspaceAdminRestAccessRule workspace admin access rules}.
 *
 * <p>In every other case it abstains rather than denies, leaving the decision to the other authorization managers
 * configured by {@link GeoServerSecurityInterceptorFilter}. This way it can only widen access for workspace
 * administrators, never restrict anyone else's.
 *
 * @see WorkspaceAdminAuthorizer
 */
@SuppressWarnings("deprecation")
public final class WorkspaceAdminAuthorizationManager implements AuthorizationManager<HttpServletRequest> {

    private SecurityMetadataSource metadata;

    public WorkspaceAdminAuthorizationManager(SecurityMetadataSource metadata) {
        this.metadata = metadata;
    }

    @Override
    @Nullable
    public AuthorizationResult authorize(
            Supplier<? extends @Nullable Authentication> authentication, HttpServletRequest request) {

        final Authentication auth = authentication.get();

        // abstain if there's no authorizer bean in the application context
        if (WorkspaceAdminAuthorizer.get().isEmpty()) {
            return ACCESS_ABSTAIN;
        }

        WorkspaceAdminAuthorizer authorizer = WorkspaceAdminAuthorizer.get().orElseThrow();

        if (!authorizer.isWorkspaceAdmin(auth)) {
            return ACCESS_ABSTAIN;
        }

        final String uri = buildRequestUrl(request);
        final HttpMethod method = HttpMethod.valueOf(request.getMethod());

        Collection<ConfigAttribute> attributes = metadata.getAttributes(request);

        boolean match = attributes.stream()
                .filter(WorkspaceAdminRestAccessRule.class::isInstance)
                .map(WorkspaceAdminRestAccessRule.class::cast)
                .anyMatch(rule -> rule.matches(uri, method));

        return match ? ACCESS_GRANTED : ACCESS_ABSTAIN;
    }

    /**
     * Replacement for {@link UrlUtils#buildRequestUrl()} because it adds a {@code ?} trailing character even if the
     * querystring is empty
     */
    private String buildRequestUrl(HttpServletRequest r) {
        String servletPath = r.getServletPath();
        String requestURI = r.getRequestURI();
        String contextPath = r.getContextPath();
        String pathInfo = r.getPathInfo();
        String queryString = r.getQueryString();
        StringBuilder url = new StringBuilder();
        if (servletPath != null) {
            url.append(servletPath);
            if (pathInfo != null) {
                url.append(pathInfo);
            }
        } else {
            url.append(requestURI.substring(contextPath.length()));
        }
        if (StringUtils.hasLength(queryString)) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }
}
