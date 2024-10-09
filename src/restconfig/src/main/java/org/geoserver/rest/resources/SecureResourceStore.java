/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.WorkspaceAdminAuthorizer;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** @see SecuredResource */
class SecureResourceStore implements ResourceStore {

    private final ResourceStore delegate;
    private final WorkspaceAdminResourceFilter resourcePathFilter;

    SecureResourceStore(ResourceStore delegate) {
        this.delegate = delegate;
        this.resourcePathFilter = new WorkspaceAdminResourceFilter(WorkspaceAdminAuthorizer.get());
    }

    //// ResourceStore methods

    @Override
    public Resource get(String path) {
        Resource resource = delegate.get(path);
        if (isAuthenticatedAsAdmin()) {
            return resource;
        }
        // return a secured decorator, it'll hide itself on getType() if not readable by
        // the current user
        return new SecuredResource(resource, this);
    }

    @Override
    public boolean remove(String path) {
        return canWrite(path) && delegate.remove(path);
    }

    @Override
    public boolean move(String source, String target) {
        return canWrite(source) && canWrite(target) && delegate.move(source, target);
    }

    @Override
    public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return delegate.getResourceNotificationDispatcher();
    }

    // support methods for SecuredResource so all logic is centralized in the store

    List<Resource> list(String path) {
        if (canRead(path)) {
            // if canRead(path) == true, can't assume it holds true for any children
            return delegate.get(path).list().stream()
                    .filter(this::canRead)
                    .map(this::wrap)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    Type getType(SecuredResource securedResource) {
        if (canRead(securedResource.path())) {
            return securedResource.delegate.getType();
        }
        return Type.UNDEFINED;
    }

    InputStream in(SecuredResource resource) {
        if (canRead(resource)) {
            return resource.delegate.in();
        }
        throw new IllegalStateException("resource not found: " + resource.path());
    }

    OutputStream out(SecuredResource resource) {
        if (canWrite(resource)) {
            return resource.delegate.out();
        }
        if (canRead(resource))
            throw new IllegalStateException("resource is read only: " + resource.path());
        throw new IllegalStateException("resource not found" + resource.path());
    }

    //// internal methods

    SecuredResource wrap(Resource resource) {
        return new SecuredResource(resource, this);
    }

    private boolean canRead(Resource resource) {
        return canRead(resource.path());
    }

    private boolean canWrite(Resource resource) {
        return canWrite(resource.path());
    }

    private boolean canRead(String path) {
        return isAuthenticatedAsAdmin() || userCanRead(path);
    }

    private boolean canWrite(String path) {
        return isAuthenticatedAsAdmin() || userCanWriteTo(path);
    }

    private boolean userCanRead(String path) {
        Authentication authentication = getAuthentication();
        return null != authentication
                && resourcePathFilter.getAccessLimits(authentication, path).canRead();
    }

    private boolean userCanWriteTo(String path) {
        Authentication authentication = getAuthentication();
        return null != authentication
                && resourcePathFilter.getAccessLimits(authentication, path).canWrite();
    }

    private boolean isAuthenticatedAsAdmin() {
        return getSecurityManager().checkAuthenticationForAdminRole();
    }

    @Nullable
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** Returns the security manager. */
    private GeoServerSecurityManager getSecurityManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }
}
