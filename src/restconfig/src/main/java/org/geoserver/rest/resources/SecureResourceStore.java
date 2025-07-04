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
import org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A secure implementation of {@link ResourceStore} that enforces access control based on workspace administration
 * privileges.
 *
 * <p>This class wraps another ResourceStore and mediates access to resources based on the current user's permissions.
 * It uses {@link WorkspaceAdminResourceFilter} to determine which resources a user can access, and returns
 * {@link SecuredResource} instances that enforce these permissions at the resource level.
 *
 * <p>For users with administrator privileges, all operations pass through directly to the delegate store. For workspace
 * administrators, access is limited to resources within workspaces they administer.
 *
 * @see SecuredResource
 * @see WorkspaceAdminResourceFilter
 */
class SecureResourceStore implements ResourceStore {

    /** The underlying resource store being secured */
    private final ResourceStore delegate;

    /** Filter that determines which resources a user can access */
    private final WorkspaceAdminResourceFilter resourcePathFilter;

    /**
     * Creates a new secure resource store wrapping the given delegate.
     *
     * @param delegate the underlying resource store to secure
     * @throws NullPointerException if the WorkspaceAdminAuthorizer bean is not available
     */
    SecureResourceStore(ResourceStore delegate) {
        this.delegate = delegate;
        this.resourcePathFilter =
                new WorkspaceAdminResourceFilter(WorkspaceAdminAuthorizer.get().orElseThrow());
    }

    //// ResourceStore methods

    /**
     * Returns a resource for the given path.
     *
     * <p>For administrators, this returns the delegate resource directly. For other users, it returns a
     * {@link SecuredResource} that enforces access control.
     *
     * @param path the path to the resource
     * @return a secured resource for non-admin users, or the delegate resource for admins
     */
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

    /**
     * Removes the resource at the given path if the current user has write access.
     *
     * @param path the path to the resource to remove
     * @return true if the resource was removed, false otherwise
     */
    @Override
    public boolean remove(String path) {
        return canWrite(path) && delegate.remove(path);
    }

    /**
     * Moves a resource from one path to another if the current user has write access to both the source and target
     * paths.
     *
     * @param source the source path
     * @param target the target path
     * @return true if the resource was moved, false otherwise
     */
    @Override
    public boolean move(String source, String target) {
        return canWrite(source) && canWrite(target) && delegate.move(source, target);
    }

    /**
     * Returns the resource notification dispatcher from the delegate store.
     *
     * @return the resource notification dispatcher
     */
    @Override
    public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return delegate.getResourceNotificationDispatcher();
    }

    // support methods for SecuredResource so all logic is centralized in the store

    /**
     * Lists all accessible child resources for the given path.
     *
     * <p>This method is used by {@link SecuredResource} to implement its {@link SecuredResource#listInternal()} method.
     *
     * @param path the path to list children for
     * @return a list of accessible child resources, or an empty list if the path is not readable
     */
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

    /**
     * Gets the type of a secured resource, respecting access control.
     *
     * <p>Returns {@link Type#UNDEFINED} if the current user doesn't have read access to the resource, making it appear
     * as if the resource doesn't exist.
     *
     * @param securedResource the secured resource to check
     * @return the resource type, or UNDEFINED if access is denied
     */
    Type getType(SecuredResource securedResource) {
        if (canRead(securedResource.path())) {
            return securedResource.delegate.getType();
        }
        return Type.UNDEFINED;
    }

    /**
     * Opens an input stream to read a secured resource if the current user has read access.
     *
     * @param resource the secured resource to read
     * @return an input stream to the resource
     * @throws IllegalStateException if the user doesn't have read access
     */
    InputStream in(SecuredResource resource) {
        if (canRead(resource)) {
            return resource.delegate.in();
        }
        throw new IllegalStateException("resource not found: " + resource.path());
    }

    /**
     * Opens an output stream to write to a secured resource if the current user has write access.
     *
     * @param resource the secured resource to write to
     * @return an output stream to the resource
     * @throws IllegalStateException if the user doesn't have write access or the resource doesn't exist
     */
    OutputStream out(SecuredResource resource) {
        if (canWrite(resource)) {
            return resource.delegate.out();
        }
        if (canRead(resource)) throw new IllegalStateException("resource is read only: " + resource.path());
        throw new IllegalStateException("resource not found" + resource.path());
    }

    //// internal methods

    /**
     * Wraps a resource with a SecuredResource.
     *
     * @param resource the resource to wrap
     * @return a secured version of the resource
     */
    SecuredResource wrap(Resource resource) {
        if (resource instanceof SecuredResource) {
            return (SecuredResource) resource;
        }
        return new SecuredResource(resource, this);
    }

    /**
     * Checks if the current user can read the given resource.
     *
     * @param resource the resource to check
     * @return true if the user can read the resource, false otherwise
     */
    private boolean canRead(Resource resource) {
        return canRead(resource.path());
    }

    /**
     * Checks if the current user can write to the given resource.
     *
     * @param resource the resource to check
     * @return true if the user can write to the resource, false otherwise
     */
    private boolean canWrite(Resource resource) {
        return canWrite(resource.path());
    }

    /**
     * Checks if the current user can read the resource at the given path.
     *
     * @param path the resource path to check
     * @return true if the user can read the resource, false otherwise
     */
    boolean canRead(String path) {
        return isAuthenticatedAsAdmin() || userCanRead(path);
    }

    /**
     * Checks if the current user can write to the resource at the given path.
     *
     * @param path the resource path to check
     * @return true if the user can write to the resource, false otherwise
     */
    boolean canWrite(String path) {
        return isAuthenticatedAsAdmin() || userCanWriteTo(path);
    }

    /**
     * Checks if the current non-admin user can read the resource at the given path.
     *
     * @param path the resource path to check
     * @return true if the user can read the resource, false otherwise
     */
    private boolean userCanRead(String path) {
        Authentication authentication = getAuthentication();
        return null != authentication
                && resourcePathFilter.getAccessLimits(authentication, path).canRead();
    }

    /**
     * Checks if the current non-admin user can write to the resource at the given path.
     *
     * @param path the resource path to check
     * @return true if the user can write to the resource, false otherwise
     */
    private boolean userCanWriteTo(String path) {
        Authentication authentication = getAuthentication();
        return null != authentication
                && resourcePathFilter.getAccessLimits(authentication, path).canWrite();
    }

    /**
     * Checks if the current user is authenticated as an administrator.
     *
     * @return true if the user is an administrator, false otherwise
     */
    private boolean isAuthenticatedAsAdmin() {
        return getSecurityManager().checkAuthenticationForAdminRole();
    }

    /**
     * Gets the current authentication.
     *
     * @return the current authentication, or null if not authenticated
     */
    @Nullable
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Returns the security manager.
     *
     * @return the GeoServer security manager
     */
    private GeoServerSecurityManager getSecurityManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }
}
