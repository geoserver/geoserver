/* (c) 2014 - 2024 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.geoserver.catalog.Catalog;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Authorizer that allows access if the user has admin rights to any workspace.
 *
 * @author Justin Deoliveira, OpenGeo
 * @see ResourceAccessManager#isWorkspaceAdmin(Authentication, Catalog)
 */
@SuppressWarnings("serial")
public class WorkspaceAdminComponentAuthorizer extends AdminComponentAuthorizer {

    /**
     * Key to cache the result of {@link #isWorkspaceAdmin(Authentication)} on the request's {@link RequestAttributes}
     * in {@link RequestAttributes#SCOPE_REQUEST request scope}
     */
    static final String REQUEST_CONTEXT_CACHE_KEY = "WORKSPACEADMIN_COMPONENT_AUTHORIZER_VALUE";

    @Override
    public boolean isAccessAllowed(Class<?> componentClass, Authentication authentication) {

        // if full admin grant access
        if (super.isAccessAllowed(componentClass, authentication)) {
            return true;
        }

        // if not authenticated deny access
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // this authorizer may be called several times per request, use a per-request cached value
        Boolean workspaceAdmin = getCachedValue();
        if (null == workspaceAdmin) {
            workspaceAdmin = isWorkspaceAdmin(authentication);
            setCachedValue(workspaceAdmin.booleanValue());
        }
        return workspaceAdmin;
    }

    /** Check if the current user has any admin privilege on at least one workspace. */
    boolean isWorkspaceAdmin(Authentication authentication) {
        ResourceAccessManager manager = getAccessManager();
        return null != manager && manager.isWorkspaceAdmin(authentication, getCatalog());
    }

    private Catalog getCatalog() {
        return getSecurityManager().getCatalog();
    }

    ResourceAccessManager getAccessManager() {
        // the secure catalog builds and owns the ResourceAccessManager
        SecureCatalogImpl secureCatalog = GeoServerApplication.get().getBeanOfType(SecureCatalogImpl.class);
        if (null == secureCatalog) return null;
        return secureCatalog.getResourceAccessManager();
    }

    void setCachedValue(boolean workspaceAdmin) {
        RequestAttributes atts = RequestContextHolder.getRequestAttributes();
        if (null != atts) {
            atts.setAttribute(REQUEST_CONTEXT_CACHE_KEY, workspaceAdmin, RequestAttributes.SCOPE_REQUEST);
        }
    }

    @Nullable
    Boolean getCachedValue() {
        RequestAttributes atts = RequestContextHolder.getRequestAttributes();
        if (null != atts) {
            return (Boolean) atts.getAttribute(REQUEST_CONTEXT_CACHE_KEY, RequestAttributes.SCOPE_REQUEST);
        }
        return null;
    }
}
