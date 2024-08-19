/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.impl.WorkspaceAdminRESTAccessRuleDAO;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.UrlUtils;

/**
 * {@link FilterInvocationSecurityMetadataSource} used as a {@link RESTfulDefinitionSourceProxy}
 * delegate to grant access to REST API resources to workspace administrators.
 *
 * <p>Values:
 *
 * <ul>
 *   <li>{@code r} = {@code GET,HEAD,OPTIONS,TRACE}
 *   <li>{@code w} = {@code POST,PUT,PATCH,DELETE}
 *   <li>{@code a} = {@code $r,$w}
 * </ul>
 *
 * <pre>
 * <cpde>
 * /rest/workspaces/{workspace}/**:*=a
 * /rest/namespaces/{workspace}/**:*=a
 * /rest/layers/{workspace}:*=a
 * /rest/resource/workspaces/{workspace}=a
 * /rest/resource/workspaces=r
 * /rest=r
 * </code>
 * </pre>
 *
 * @see RESTfulDefinitionSource
 * @see WorkspaceAdminRESTAccessRuleDAO
 */
public class WorkspaceAdminRestfulDefinitionSource
        implements FilterInvocationSecurityMetadataSource {

    private WorkspaceAdminAuthorizer authorizer;

    public WorkspaceAdminRestfulDefinitionSource(WorkspaceAdminAuthorizer authorizer) {
        this.authorizer = authorizer;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return HttpServletRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object)
            throws IllegalArgumentException {
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
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return authorizer.getAccessRules().stream()
                .map(ConfigAttribute.class::cast)
                .collect(Collectors.toList());
    }
}
