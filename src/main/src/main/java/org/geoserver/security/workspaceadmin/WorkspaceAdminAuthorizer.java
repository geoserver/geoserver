/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import static java.util.Objects.requireNonNull;
import static org.geoserver.platform.GeoServerExtensions.bean;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RESTfulDefinitionSource;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WorkspaceAccessLimits;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Determines whether an authenticated user is a workspace administrator, and whether a request URI and HTTP method
 * match the configured workspace admin REST access rules.
 *
 * <p>Shared by the web UI ({@code WorkspaceAdminComponentAuthorizer}) and the REST API
 * ({@link WorkspaceAdminRestfulDefinitionSource}, {@link WorkspaceAdminAuthorizationManager}). The access rules are
 * loaded by {@link WorkspaceAdminRESTAccessRuleDAO} from {@code security/rest.workspaceadmin.properties}.
 *
 * <p>The workspace admin check delegates to {@link ResourceAccessManager#isWorkspaceAdmin(Authentication, Catalog)} and
 * caches the result in request scope, since it can be called several times while processing a single request.
 *
 * @see RESTfulDefinitionSource
 * @see WorkspaceAdminRESTAccessRuleDAO
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
     * Returns true if the user is a full administrator, or is a workspace administrator and the URI and method match
     * one of the configured access rules.
     */
    public boolean canAccess(Authentication authentication, String requestUri, HttpMethod method) {
        return isAdmin() || (matches(requestUri, method) && isWorkspaceAdmin(authentication));
    }

    List<WorkspaceAdminRestAccessRule> getAccessRules() {
        return List.copyOf(dao.getRules());
    }

    Optional<WorkspaceAdminRestAccessRule> findMatchingRule(String url, HttpMethod method) {
        return dao.getRules().stream().filter(rule -> rule.matches(url, method)).findFirst();
    }

    private boolean matches(String uri, HttpMethod method) {
        return findMatchingRule(uri, method).isPresent();
    }

    private boolean isAdmin() {
        GeoServerSecurityManager manager = requireNonNull(bean(GeoServerSecurityManager.class));
        return manager.checkAuthenticationForAdminRole();
    }

    /**
     * Determines if the given authentication represents a workspace administrator, caching the result in request scope
     * since this can be called several times per request. Anonymous or unauthenticated users are never workspace
     * administrators.
     */
    public boolean isWorkspaceAdmin(Authentication authentication) {
        Boolean workspaceAdmin = false;
        if (isFullyAuthenticated(authentication)) {
            workspaceAdmin = getRequestScopeCachedValue().orElseGet(() -> checkIsWorkspaceAdmin(authentication));
            setRequestScopeCachedValue(workspaceAdmin);
        }
        return workspaceAdmin;
    }

    private boolean checkIsWorkspaceAdmin(Authentication authentication) {
        ResourceAccessManager accessManager = getAccessManager();
        if (accessManager == null) {
            return false;
        }
        Catalog catalog = getCatalog();
        return accessManager.isWorkspaceAdmin(authentication, catalog);
    }

    boolean isFullyAuthenticated(Authentication auth) {
        return null != auth && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    void setRequestScopeCachedValue(boolean workspaceAdmin) {
        RequestAttributes atts = RequestContextHolder.getRequestAttributes();
        if (null != atts) {
            atts.setAttribute(WSADMIN_REQUEST_CONTEXT_KEY, workspaceAdmin, SCOPE_REQUEST);
        }
    }

    private Optional<Boolean> getRequestScopeCachedValue() {
        RequestAttributes atts = RequestContextHolder.getRequestAttributes();
        if (atts == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((Boolean) atts.getAttribute(WSADMIN_REQUEST_CONTEXT_KEY, SCOPE_REQUEST));
    }

    protected ResourceAccessManager getAccessManager() {
        // the secure catalog builds and owns the ResourceAccessManager
        return getSecureCatalog().getResourceAccessManager();
    }

    private SecureCatalogImpl getSecureCatalog() {
        return requireNonNull(bean(SecureCatalogImpl.class));
    }

    private Catalog getCatalog() {
        return requireNonNull(bean(GeoServer.class)).getCatalog();
    }

    /** Returns the access limits for the given workspace, or null if the workspace does not exist. */
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
