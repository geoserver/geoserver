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
 * Spring Security metadata source that provides access rules for workspace administrators.
 *
 * <p>This class implements {@link FilterInvocationSecurityMetadataSource} and is used as a delegate by the
 * {@link RESTfulDefinitionSourceProxy} to determine which REST API resources workspace administrators are allowed to
 * access. It works closely with the {@link WorkspaceAdminAuthorizer} to match HTTP requests against defined access
 * rules.
 *
 * <p>When a request comes in, this class:
 *
 * <ol>
 *   <li>Extracts the request URI and HTTP method
 *   <li>Delegates to the WorkspaceAdminAuthorizer to find a matching rule
 *   <li>Returns the rule as a ConfigAttribute if found, or an empty list if no rule matches
 * </ol>
 *
 * <p>The actual authorization decision is made by the {@link WorkspaceAdminAuthorizationManager}, which uses this class
 * to get the applicable rules and then checks if the user has workspace administrator privileges.
 *
 * <h3>Rule Format</h3>
 *
 * <p>Rules are defined in properties files with the format:
 *
 * <pre>
 * /url/pattern=METHOD1,METHOD2,...
 * </pre>
 *
 * <p>Where methods can use these shorthand values:
 *
 * <ul>
 *   <li>{@code r} = Read operations ({@code GET, HEAD, OPTIONS, TRACE})
 *   <li>{@code w} = Write operations ({@code POST, PUT, PATCH, DELETE})
 *   <li>{@code rw} = All operations ({@code r + w})
 * </ul>
 *
 * <p>The default rules in rest.workspaceadmin.properties allow workspace administrators to manage resources within
 * their assigned workspaces, while endpoints not matching any pattern fall back to the global REST security
 * configuration, which typically restricts access to administrators only.
 *
 * <h3>Default Rules</h3>
 *
 * <p>By default, workspace administrators have access to:
 *
 * <pre>
 * # Workspace and catalog endpoints
 * /rest/workspaces.{ext}=r
 * /rest/workspaces=r
 * /rest/workspaces/{workspace}.{ext}=r,PUT  # Can update but not rename workspace
 * /rest/workspaces/{workspace}=r,PUT  # Can update but not rename workspace
 * /rest/workspaces/{workspace}/**=rw
 * /rest/namespaces.{ext}=r
 * /rest/namespaces=r
 * /rest/namespaces/{namespace}.{ext}=r,PUT  # Can update but not rename namespace
 * /rest/namespaces/{namespace}=r,PUT  # Can update but not rename namespace
 * /rest/namespaces/{namespace}/**=rw
 * /rest/layers/**=rw  # Filtered by secure catalog to only show layers in managed workspaces
 * /rest/styles.{ext}=r  # Read-only access to global styles listing
 * /rest/styles/**=r  # Read-only access to global styles
 *
 * # Resource access
 * /rest/resource/workspaces=r
 * /rest/resource/workspaces/{workspace}/**=rw
 * /rest/resource/**=r
 *
 * # User account management
 * /rest/security/self/**=rw
 *
 * # General service endpoints
 * /rest/fonts.{ext}=r
 * /rest/fonts/**=r
 * /rest=r
 * /rest/=r
 * /rest.{ext}=r
 * /rest/index=r
 * /rest/index.{ext}=r
 * </pre>
 *
 * <p>Note that workspace administrators cannot:
 *
 * <ul>
 *   <li>Rename workspaces or namespaces they administer (can only modify other properties)
 *   <li>Change the default workspace or namespace settings
 *   <li>Modify global styles (read-only access)
 * </ul>
 *
 * <p>The URL patterns use Spring's AntPathMatcher syntax, with the {workspace} variable being matched against
 * workspaces the administrator has access to.
 *
 * @see RESTfulDefinitionSource
 * @see RESTfulDefinitionSourceProxy
 * @see WorkspaceAdminRESTAccessRuleDAO
 * @see WorkspaceAdminAuthorizationManager
 * @see WorkspaceAdminAuthorizer
 */
public class WorkspaceAdminRestfulDefinitionSource implements FilterInvocationSecurityMetadataSource {

    /**
     * The workspace administrator authorizer that maintains the access rules and provides rule matching functionality.
     */
    private WorkspaceAdminAuthorizer authorizer;

    /**
     * Creates a new definition source for workspace administrator REST API access.
     *
     * @param authorizer the authorizer that will provide and match access rules
     */
    public WorkspaceAdminRestfulDefinitionSource(WorkspaceAdminAuthorizer authorizer) {
        this.authorizer = authorizer;
    }

    /**
     * Checks if this definition source supports the given class.
     *
     * <p>This implementation only supports HTTP servlet requests.
     *
     * @param clazz the class to check
     * @return true if the class is assignable from HttpServletRequest, false otherwise
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return HttpServletRequest.class.isAssignableFrom(clazz);
    }

    /**
     * Returns the security attributes (access rules) that apply to the given object.
     *
     * <p>This method:
     *
     * <ol>
     *   <li>Extracts the URI and HTTP method from the request
     *   <li>Delegates to the {@link WorkspaceAdminAuthorizer} to find a matching rule
     *   <li>Returns the rule as a ConfigAttribute if found, or an empty list if no match
     * </ol>
     *
     * @param object the object to get attributes for (must be an HttpServletRequest)
     * @return a collection containing the matching rule, or an empty collection if no rule matches
     * @throws IllegalArgumentException if the object is not an HttpServletRequest
     */
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

    /**
     * Returns all configuration attributes (access rules) defined for workspace administrators.
     *
     * <p>This method returns all access rules from the {@link WorkspaceAdminAuthorizer}, converting them to
     * ConfigAttribute instances.
     *
     * @return a collection of all workspace administrator access rules
     */
    @Override
    public List<ConfigAttribute> getAllConfigAttributes() {
        return authorizer.getAccessRules().stream()
                .map(ConfigAttribute.class::cast)
                .collect(Collectors.toList());
    }
}
