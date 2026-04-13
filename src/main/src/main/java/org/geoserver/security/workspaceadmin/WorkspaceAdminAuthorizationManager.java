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
 * Authorization manager specifically for handling workspace administrator access permissions.
 *
 * <p>This class is used by the {@link GeoServerSecurityInterceptorFilter} as one of several potential authorization
 * strategies when evaluating access to protected resources in GeoServer. It focuses specifically on the workspace
 * administrator role and permissions.
 *
 * <p>The class works in tandem with the {@link WorkspaceAdminAuthorizer} to determine if a user has workspace
 * administrator privileges and whether they should be granted access to a specific resource based on URL patterns
 * defined in the {@link WorkspaceAdminRESTAccessRuleDAO}.
 *
 * <p>Authorization proceeds as follows:
 *
 * <ol>
 *   <li>First checks if the {@link WorkspaceAdminAuthorizer} service is available
 *   <li>Extracts the request URI and HTTP method
 *   <li>Checks if there's a matching {@link WorkspaceAdminRestAccessRule} for the URI and method
 *   <li>If a rule matches, confirms that the user is a workspace administrator
 *   <li>Returns ACCESS_GRANTED if all conditions are met, otherwise ACCESS_ABSTAIN to allow other authorization
 *       managers to evaluate the request
 * </ol>
 *
 * <p>By returning ACCESS_ABSTAIN rather than ACCESS_DENIED when a user isn't a workspace administrator, this manager
 * allows the request to be evaluated by other authorization managers in the filter chain, such as those handling
 * role-based or authenticated access.
 *
 * @see WorkspaceAdminAuthorizer
 * @see WorkspaceAdminRestAccessRule
 * @see GeoServerSecurityInterceptorFilter
 */
@SuppressWarnings("deprecation")
public final class WorkspaceAdminAuthorizationManager implements AuthorizationManager<HttpServletRequest> {

    /**
     * The security metadata source containing access rules for workspace administrators. This provides the
     * configuration attributes (rules) to be checked against the request.
     */
    private SecurityMetadataSource metadata;

    /**
     * Creates a new authorization manager for workspace administrators.
     *
     * @param metadata the security metadata source containing access rules for workspace administrators
     */
    public WorkspaceAdminAuthorizationManager(SecurityMetadataSource metadata) {
        this.metadata = metadata;
    }

    /**
     * Evaluates if the current request should be authorized based on workspace administrator privileges.
     *
     * <p>This method implements the core authorization logic for workspace administrators by:
     *
     * <ul>
     *   <li>Obtaining the WorkspaceAdminAuthorizer service
     *   <li>Determining if any workspace admin access rules match the requested URI and HTTP method
     *   <li>Verifying that the user has workspace administrator privileges
     * </ul>
     *
     * <p>The method will return:
     *
     * <ul>
     *   <li>{@link GeoServerSecurityInterceptorFilter#ACCESS_ABSTAIN} if the WorkspaceAdminAuthorizer is not available,
     *       if no matching rules are found, or if the user is not a workspace administrator
     *   <li>{@link GeoServerSecurityInterceptorFilter#ACCESS_GRANTED} if a matching rule is found and the user is a
     *       workspace administrator
     * </ul>
     *
     * @param authentication a supplier for the authentication object representing the current user
     * @param request the HTTP request being evaluated
     * @return an authorization decision indicating whether access should be granted, denied, or abstained
     */
    @Override
    @Nullable
    public AuthorizationResult authorize(
            Supplier<? extends @Nullable Authentication> authentication, HttpServletRequest request) {

        final Authentication auth = authentication.get();

        // if the authorizer service isn't available, abstain from making a decision
        if (WorkspaceAdminAuthorizer.get().isEmpty()) {
            return ACCESS_ABSTAIN;
        }

        WorkspaceAdminAuthorizer authorizer = WorkspaceAdminAuthorizer.get().orElseThrow();

        // if the user is not a workspace administrator, just abstain
        if (!authorizer.isWorkspaceAdmin(auth)) {
            return ACCESS_ABSTAIN;
        }

        // extract the request URI and HTTP method
        final String uri = buildRequestUrl(request);
        final HttpMethod method = HttpMethod.valueOf(request.getMethod());

        // get the security attributes (rules) that apply to this request
        Collection<ConfigAttribute> attributes = metadata.getAttributes(request);

        // check if any workspace admin rules match the request URI and method
        boolean match = attributes.stream()
                .filter(WorkspaceAdminRestAccessRule.class::isInstance)
                .map(WorkspaceAdminRestAccessRule.class::cast)
                .anyMatch(rule -> rule.matches(uri, method));

        // grant access if a rule matches, otherwise abstain
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
