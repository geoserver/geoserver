/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import static org.geoserver.security.filter.GeoServerSecurityInterceptorFilter.ACCESS_ABSTAIN;
import static org.geoserver.security.filter.GeoServerSecurityInterceptorFilter.ACCESS_DENIED;
import static org.geoserver.security.filter.GeoServerSecurityInterceptorFilter.ACCESS_GRANTED;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.function.Supplier;
import org.geoserver.security.filter.GeoServerSecurityInterceptorFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.util.UrlUtils;

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
 *   <li>Verifies that the authentication represents a fully authenticated user (not anonymous)
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
     *   <li>Checking if the user is fully authenticated
     *   <li>Determining if any workspace admin access rules match the requested URI and HTTP method
     *   <li>Verifying that the user has workspace administrator privileges
     * </ul>
     *
     * <p>The method will return:
     *
     * <ul>
     *   <li>{@link GeoServerSecurityInterceptorFilter#ACCESS_ABSTAIN} if the WorkspaceAdminAuthorizer is not available
     *       or if no matching rules are found
     *   <li>{@link GeoServerSecurityInterceptorFilter#ACCESS_DENIED} if the user is not fully authenticated
     *   <li>{@link GeoServerSecurityInterceptorFilter#ACCESS_GRANTED} if a matching rule is found and the user is a
     *       workspace administrator
     * </ul>
     *
     * @param authentication a supplier for the authentication object representing the current user
     * @param request the HTTP request being evaluated
     * @return an authorization decision indicating whether access should be granted, denied, or abstained
     */
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, HttpServletRequest request) {
        // Get the authentication object
        final Authentication auth = authentication.get();

        // If the authorizer service isn't available, abstain from making a decision
        if (WorkspaceAdminAuthorizer.get().isEmpty()) {
            return ACCESS_ABSTAIN;
        }

        // Get the workspace admin authorizer service
        WorkspaceAdminAuthorizer authorizer = WorkspaceAdminAuthorizer.get().orElseThrow();

        // Deny access if the user is not fully authenticated (e.g., anonymous)
        if (!authorizer.isFullyAuthenticated(auth)) {
            return ACCESS_DENIED;
        }

        // Extract the request URI and HTTP method
        final String uri = UrlUtils.buildRequestUrl(request);
        final HttpMethod method = HttpMethod.valueOf(request.getMethod());

        // Get the security attributes (rules) that apply to this request
        Collection<ConfigAttribute> attributes = metadata.getAttributes(request);

        // Check if any workspace admin rules match the request URI and method
        boolean match = attributes.stream()
                .filter(WorkspaceAdminRestAccessRule.class::isInstance)
                .map(WorkspaceAdminRestAccessRule.class::cast)
                .anyMatch(rule -> rule.matches(uri, method));

        // Grant access if a rule matches and the user is a workspace admin, otherwise abstain
        // defer the call to isWorkspaceAdmin() as the last step as it can be slow
        return match && authorizer.isWorkspaceAdmin(auth) ? ACCESS_GRANTED : ACCESS_ABSTAIN;
    }
}
