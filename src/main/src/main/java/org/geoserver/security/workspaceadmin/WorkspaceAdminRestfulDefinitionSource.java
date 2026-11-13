/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.security.RESTfulDefinitionSource;
import org.geoserver.security.RESTfulDefinitionSourceProxy;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.UrlUtils;

/**
 * Security metadata source that returns the {@link WorkspaceAdminRestAccessRule} matching a request's URI and HTTP
 * method, or nothing when no rule matches.
 *
 * <p>Registered as a delegate of {@link RESTfulDefinitionSourceProxy} alongside the standard
 * {@link RESTfulDefinitionSource}. The authorization decision itself is made by
 * {@link WorkspaceAdminAuthorizationManager}, which also checks that the user is a workspace administrator.
 *
 * @see WorkspaceAdminRESTAccessRuleDAO
 */
@SuppressWarnings("deprecation")
public class WorkspaceAdminRestfulDefinitionSource implements FilterInvocationSecurityMetadataSource {

    private WorkspaceAdminAuthorizer authorizer;

    public WorkspaceAdminRestfulDefinitionSource(WorkspaceAdminAuthorizer authorizer) {
        this.authorizer = authorizer;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return HttpServletRequest.class.isAssignableFrom(clazz);
    }

    /** Returns the first access rule matching the request, or an empty collection if none matches. */
    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        HttpServletRequest request = (HttpServletRequest) object;
        String uri = UrlUtils.buildRequestUrl(request);
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        return authorizer
                .findMatchingRule(uri, method)
                .map(ConfigAttribute.class::cast)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<ConfigAttribute> getAllConfigAttributes() {
        return authorizer.getAccessRules().stream()
                .map(ConfigAttribute.class::cast)
                .collect(Collectors.toList());
    }
}
