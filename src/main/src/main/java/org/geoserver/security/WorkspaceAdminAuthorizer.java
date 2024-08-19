/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static java.util.Objects.requireNonNull;
import static org.geoserver.platform.GeoServerExtensions.bean;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.impl.AbstractAccessRuleDAO;
import org.geoserver.security.impl.WorkspaceAdminRESTAccessRuleDAO;
import org.geoserver.security.impl.WorkspaceAdminRestAccessRule;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

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
 * * @implNote {@link GeoServerExtensions} is used to get a hold on the {@link Catalog} and the
 * {@link SecureCatalogImpl}, the former is expected to filter out which workspaces an {@code
 * Authentication} can see, the later giving access to the {@link ResourceAccessManager}.
 *
 * @see RESTfulDefinitionSource
 * @see WorkspaceAdminRESTAccessRuleDAO
 */
public class WorkspaceAdminAuthorizer {

    /**
     * Key to cache the result of {@link #isWorkspaceAdmin(Authentication)} on the request's {@link
     * RequestAttributes} in {@link RequestAttributes#SCOPE_REQUEST request scope}
     */
    static final String WSADMIN_REQUEST_CONTEXT_KEY = "WORKSPACEADMIN_AUTHORIZER_VALUE";

    private AbstractAccessRuleDAO<WorkspaceAdminRestAccessRule> dao;

    public WorkspaceAdminAuthorizer() {
        this(WorkspaceAdminRESTAccessRuleDAO.get());
    }

    public WorkspaceAdminAuthorizer(AbstractAccessRuleDAO<WorkspaceAdminRestAccessRule> dao) {
        Objects.requireNonNull(dao, "AbstractAccessRuleDAO<WorkspaceAdminRestAccessRule> is null");
        this.dao = dao;
    }

    public static WorkspaceAdminAuthorizer get() {
        return requireNonNull(bean(WorkspaceAdminAuthorizer.class));
    }

    public boolean canAccess(Authentication authentication, String requestUri, HttpMethod method) {
        return isAdmin() || (matches(requestUri, method) && isWorkspaceAdmin(authentication));
    }

    List<WorkspaceAdminRestAccessRule> getAccessRules() {
        return List.copyOf(dao.getRules());
    }

    /** @return the rule that first matches the url, following the rule order definition */
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

    public boolean isFullyAuthenticated(Authentication auth) {
        return null != auth
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    void setRequestScopeCachedValue(boolean workspaceAdmin) {
        RequestAttributes atts = RequestContextHolder.getRequestAttributes();
        if (null != atts) {
            atts.setAttribute(
                    WSADMIN_REQUEST_CONTEXT_KEY, workspaceAdmin, RequestAttributes.SCOPE_REQUEST);
        }
    }

    @Nullable
    Boolean getRequestScopeCachedValue() {
        RequestAttributes atts = RequestContextHolder.getRequestAttributes();
        if (null != atts) {
            return (Boolean)
                    atts.getAttribute(WSADMIN_REQUEST_CONTEXT_KEY, RequestAttributes.SCOPE_REQUEST);
        }
        return null;
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

    @Nullable
    public WorkspaceAccessLimits getWorkspaceAccessLimits(
            Authentication authentication, final String workspaceName) {
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
