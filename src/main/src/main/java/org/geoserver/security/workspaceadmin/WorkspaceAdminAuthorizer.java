/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import static java.util.Objects.requireNonNull;
import static org.geoserver.platform.GeoServerExtensions.bean;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RESTfulDefinitionSource;
import org.geoserver.security.RESTfulDefinitionSourceProxy;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WorkspaceAccessLimits;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Central authorization component for workspace administrators in GeoServer.
 *
 * <p>This class provides a unified authorization mechanism for determining whether a user with workspace administration
 * privileges can access specific resources, both in the web UI and the REST API. It serves as the cornerstone of
 * GeoServer's fine-grained access control system for workspace administrators.
 *
 * <p>The WorkspaceAdminAuthorizer:
 *
 * <ul>
 *   <li>Determines if a user is a workspace administrator
 *   <li>Identifies which workspaces a user can administer
 *   <li>Evaluates access permissions for REST API endpoints and resources
 *   <li>Manages performance through request-scoped caching of authorization decisions
 * </ul>
 *
 * <p><strong>REST API Access Control</strong>
 *
 * <p>For REST API access, this class works with {@link WorkspaceAdminRestfulDefinitionSource} as a delegate to the
 * {@link RESTfulDefinitionSourceProxy} to enforce URL pattern-based security rules for workspace administrators. It
 * uses the access rule values:
 *
 * <ul>
 *   <li>{@code r} = Read operations ({@code GET, HEAD, OPTIONS, TRACE})
 *   <li>{@code w} = Write operations ({@code POST, PUT, PATCH, DELETE})
 *   <li>{@code rw} = All operations ({@code r + w})
 * </ul>
 *
 * <p><strong>Default Access Rules</strong>
 *
 * <p>By default, workspace administrators have these permissions:
 *
 * <pre>
 * /rest/workspaces/{workspace}/**:*=rw       (Full control over their workspaces)
 * /rest/namespaces/{workspace}/**:*=rw       (Full control over related namespaces)
 * /rest/layers/{workspace}:*=rw              (Full control over workspace layers)
 * /rest/resource/workspaces/{workspace}=rw   (Full control over workspace resources)
 * /rest/resource/workspaces=r               (Read-only access to workspaces list)
 * /rest=r                                   (Read-only access to REST API root)
 * </pre>
 *
 * <p>This pattern-based system allows for flexible and fine-grained control over which REST endpoints and resources a
 * workspace administrator can access.
 *
 * <p><strong>Implementation Details</strong>
 *
 * <p>The authorizer uses {@link GeoServerExtensions} to access the {@link Catalog} and {@link SecureCatalogImpl}. The
 * catalog filters which workspaces an {@code Authentication} can see, while the secure catalog provides access to the
 * {@link ResourceAccessManager} which determines administrator privileges for specific workspaces.
 *
 * @see RESTfulDefinitionSource
 * @see WorkspaceAdminRESTAccessRuleDAO
 * @see WorkspaceAdminRestfulDefinitionSource
 * @see ResourceAccessManager#isWorkspaceAdmin(Authentication, Catalog)
 */
public class WorkspaceAdminAuthorizer {

    /**
     * Key to cache the result of {@link #isWorkspaceAdmin(Authentication)} on the request's {@link RequestAttributes}
     * in {@link RequestAttributes#SCOPE_REQUEST request scope}
     */
    static final String WSADMIN_REQUEST_CONTEXT_KEY = "WORKSPACEADMIN_AUTHORIZER_VALUE";

    private WorkspaceAdminRESTAccessRuleDAO dao;

    public WorkspaceAdminAuthorizer(WorkspaceAdminRESTAccessRuleDAO dao) {
        Objects.requireNonNull(dao, "AbstractAccessRuleDAO<WorkspaceAdminRestAccessRule> is null");
        this.dao = dao;
    }

    public static Optional<WorkspaceAdminAuthorizer> get() {
        return Optional.ofNullable(bean(WorkspaceAdminAuthorizer.class));
    }

    /**
     * Determines if the given authentication can access the specified URI and HTTP method. Access is granted if the
     * user is a GeoServer admin or if the user is a workspace admin and the URI/method matches one of the configured
     * access rules.
     *
     * @param authentication the authentication to check
     * @param requestUri the request URI to check
     * @param method the HTTP method being used
     * @return true if access should be granted, false otherwise
     */
    public boolean canAccess(Authentication authentication, String requestUri, HttpMethod method) {
        return isAdmin() || (matches(requestUri, method) && isWorkspaceAdmin(authentication));
    }

    /**
     * Returns an immutable list of all configured workspace admin access rules.
     *
     * @return list of all access rules
     */
    List<WorkspaceAdminRestAccessRule> getAccessRules() {
        return List.copyOf(dao.getRules());
    }

    /**
     * Finds the first rule that matches the given URL and HTTP method.
     *
     * @param url the URL to match
     * @param method the HTTP method to match
     * @return an Optional containing the first matching rule, or empty if no rules match
     */
    Optional<WorkspaceAdminRestAccessRule> findMatchingRule(String url, HttpMethod method) {
        return dao.getRules().stream().filter(rule -> rule.matches(url, method)).findFirst();
    }

    /**
     * Checks if the given URI and method match any configured access rule.
     *
     * @param uri the URI to check
     * @param method the HTTP method to check
     * @return true if a matching rule is found, false otherwise
     */
    private boolean matches(String uri, HttpMethod method) {
        return findMatchingRule(uri, method).isPresent();
    }

    /**
     * Determines if the current user has the admin role.
     *
     * @return true if the user has the admin role, false otherwise
     */
    private boolean isAdmin() {
        GeoServerSecurityManager manager = requireNonNull(bean(GeoServerSecurityManager.class));
        return manager.checkAuthenticationForAdminRole();
    }

    /**
     * Determines if the given authentication represents a workspace administrator. The result is cached per request for
     * performance.
     *
     * @param authentication the authentication to check
     * @return true if the authentication represents a workspace administrator, false otherwise
     */
    public boolean isWorkspaceAdmin(Authentication authentication) {
        // if not authenticated deny access
        if (!isFullyAuthenticated(authentication)) {
            return false;
        }
        // this authorizer may be called several times per request, use a per-request
        // cached value
        Boolean workspaceAdmin = getRequestScopeCachedValue();
        if (null == workspaceAdmin) {
            ResourceAccessManager accessManager = getAccessManager();
            if (null == accessManager) {
                return false;
            }
            Catalog catalog = getCatalog();
            workspaceAdmin = accessManager.isWorkspaceAdmin(authentication, catalog);
            setRequestScopeCachedValue(workspaceAdmin.booleanValue());
        }
        return workspaceAdmin.booleanValue();
    }

    /**
     * Checks if the given authentication represents a fully authenticated user (authenticated and not anonymous).
     *
     * @param auth the authentication to check
     * @return true if the authentication is valid and not anonymous, false otherwise
     */
    public boolean isFullyAuthenticated(Authentication auth) {
        return null != auth && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    /**
     * Caches the workspace admin status of a user in the current request scope, since
     * {@link #isWorkspaceAdmin(Authentication)} can be called multiple times for a single request.
     *
     * @param workspaceAdmin true if the user is a workspace admin, false otherwise
     */
    void setRequestScopeCachedValue(boolean workspaceAdmin) {
        RequestAttributes atts = RequestContextHolder.getRequestAttributes();
        if (null != atts) {
            atts.setAttribute(WSADMIN_REQUEST_CONTEXT_KEY, workspaceAdmin, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * Retrieves the cached workspace admin status from the current request scope.
     *
     * @return the cached status if available, null otherwise
     */
    @Nullable
    Boolean getRequestScopeCachedValue() {
        RequestAttributes atts = RequestContextHolder.getRequestAttributes();
        if (null != atts) {
            return (Boolean) atts.getAttribute(WSADMIN_REQUEST_CONTEXT_KEY, RequestAttributes.SCOPE_REQUEST);
        }
        return null;
    }

    /**
     * Gets the ResourceAccessManager from the SecureCatalog.
     *
     * @return the resource access manager
     */
    protected ResourceAccessManager getAccessManager() {
        // the secure catalog builds and owns the ResourceAccessManager
        return getSecureCatalog().getResourceAccessManager();
    }

    /**
     * Gets the SecureCatalogImpl from the application context.
     *
     * @return the secure catalog implementation
     * @throws NullPointerException if the secure catalog is not found
     */
    private SecureCatalogImpl getSecureCatalog() {
        return requireNonNull(bean(SecureCatalogImpl.class));
    }

    /**
     * Gets the catalog from the GeoServer instance.
     *
     * @return the catalog
     * @throws NullPointerException if either GeoServer or the catalog is not found
     */
    private Catalog getCatalog() {
        return requireNonNull(bean(GeoServer.class)).getCatalog();
    }

    /**
     * Gets the workspace access limits for the given authentication and workspace.
     *
     * @param authentication the authentication to check
     * @param workspaceName the name of the workspace
     * @return the workspace access limits, or null if the workspace doesn't exist or the user has no access
     */
    @Nullable
    public WorkspaceAccessLimits getWorkspaceAccessLimits(Authentication authentication, final String workspaceName) {
        WorkspaceAccessLimits wsAccessLimits = null;
        Catalog catalog = getCatalog();
        WorkspaceInfo workspace = catalog.getWorkspaceByName(workspaceName);
        if (workspace != null) {
            ResourceAccessManager accessManager = getAccessManager();
            wsAccessLimits = accessManager.getAccessLimits(authentication, workspace);
        }
        return wsAccessLimits;
    }
}
