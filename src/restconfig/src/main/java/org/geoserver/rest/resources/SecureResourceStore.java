/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceNotificationDispatcher;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.rest.RestException;
import org.geoserver.rest.resources.WorkspaceAdminResourceFilter.ResourceAccess;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
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
 * <p>For users with full admin rights, all operations pass through directly to the delegate store. For workspace
 * administrators, access is limited to resources within workspaces they administer.
 *
 * <p>The {@link ResourceStore#get(String)} contract requires returning a {@link Resource} with
 * {@link Resource.Type#UNDEFINED} for non-existent paths rather than throwing an exception. Because of this,
 * {@link #get(String)} cannot reject inaccessible paths upfront, it returns a {@link SecuredResource} that defers
 * access checks to the actual operation ({@link SecuredResource#in()}, {@link SecuredResource#out()},
 * {@link SecuredResource#file()}, {@link SecuredResource#dir()}). These methods throw {@link RestException} with the
 * appropriate HTTP status (404 if the resource is not visible, 403 if it is visible but read-only).
 *
 * @see SecuredResource
 * @see WorkspaceAdminResourceFilter
 * @see ResourceController
 */
class SecureResourceStore implements ResourceStore {

    /** The underlying resource store being secured */
    private final ResourceStore delegate;

    /** Filter that determines which resources a user can access */
    private final WorkspaceAdminResourceFilter resourcePathFilter;

    /** Creates a new secure resource store wrapping the given delegate. */
    SecureResourceStore(ResourceStore delegate) {
        this.delegate = delegate;
        WorkspaceAdminAuthorizer workspaceAdminAuthorizer =
                WorkspaceAdminAuthorizer.get().orElseThrow();
        this.resourcePathFilter = new WorkspaceAdminResourceFilter(workspaceAdminAuthorizer);
    }

    // ResourceStore methods //

    /**
     * Returns a resource for the given path.
     *
     * <p>For administrators, this returns the delegate resource directly. For other users, it returns a
     * {@link SecuredResource} that enforces access control.
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

    /** Removes the resource at the given path if the current user has write access. */
    @Override
    public boolean remove(String path) {
        return canWrite(path) && delegate.remove(path);
    }

    /**
     * Moves a resource from one path to another if the current user has write access to both the source and target
     * paths.
     */
    @Override
    public boolean move(String source, String target) {
        return canWrite(source) && canWrite(target) && delegate.move(source, target);
    }

    /** @return the resource notification dispatcher from the delegate store */
    @Override
    public ResourceNotificationDispatcher getResourceNotificationDispatcher() {
        return delegate.getResourceNotificationDispatcher();
    }

    // support methods for SecuredResource so all logic is centralized in the store //

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
            List<Resource> delegateChildren = delegate.get(path).list();
            List<Resource> visible = new ArrayList<>();
            for (Resource child : delegateChildren) {
                if (canRead(child)) {
                    visible.add(wrap(child));
                }
            }
            return visible;
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
     * @throws RestException 404 if the user doesn't have read access
     */
    InputStream in(SecuredResource resource) {
        if (canRead(resource)) {
            return resource.delegate.in();
        }
        throw notFound(resource);
    }

    /**
     * Opens an output stream to write to a secured resource if the current user has write access.
     *
     * @throws RestException 403 if the resource is read-only, 404 if not accessible
     */
    OutputStream out(SecuredResource resource) {
        if (canWrite(resource)) {
            return resource.delegate.out();
        }
        throw denyWrite(resource);
    }

    File file(SecuredResource resource) {
        if (canWrite(resource)) {
            return resource.delegate.file();
        }
        throw denyWrite(resource);
    }

    File dir(SecuredResource resource) {
        if (canWrite(resource)) {
            return resource.delegate.dir();
        }
        throw denyWrite(resource);
    }

    // internal methods //

    private RestException denyWrite(SecuredResource resource) {
        if (canRead(resource)) {
            return new RestException("Resource is read only: " + resource.path(), HttpStatus.FORBIDDEN);
        }
        return notFound(resource);
    }

    private RestException notFound(SecuredResource resource) {
        return new RestException("Resource not found: " + resource.path(), HttpStatus.NOT_FOUND);
    }

    /**
     * Wraps a resource with a SecuredResource.
     *
     * @param resource the resource to wrap
     * @return a secured version of the resource
     */
    SecuredResource wrap(Resource resource) {
        if (resource instanceof SecuredResource secured) {
            return secured;
        }
        return new SecuredResource(resource, this);
    }

    /** @return true if the user can read the resource, false otherwise */
    private boolean canRead(Resource resource) {
        return canRead(resource.path());
    }

    /** @return true if the user can write to the resource, false otherwise */
    private boolean canWrite(Resource resource) {
        return canWrite(resource.path());
    }

    /** @return true if the user can read the resource, false otherwise */
    boolean canRead(String path) {
        boolean isFullAdmin = isAuthenticatedAsAdmin();
        return isFullAdmin || userCanRead(path);
    }

    /** @return true if the user can write to the resource, false otherwise */
    boolean canWrite(String path) {
        boolean isFullAdmin = isAuthenticatedAsAdmin();
        return isFullAdmin || userCanWriteTo(path);
    }

    /** @return true if the user can read the resource, false otherwise */
    private boolean userCanRead(String path) {
        ResourceAccess access = getAccess(path);
        return access.canRead();
    }
    /** @return true if the user can write to the resource according to #resourcePathFilter, false otherwise */
    private boolean userCanWriteTo(String path) {
        ResourceAccess access = getAccess(path);
        return access.canWrite();
    }

    private ResourceAccess getAccess(String path) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return ResourceAccess.NONE;
        }
        return resourcePathFilter.getAccessLimits(authentication, path);
    }

    /** @return true if the user is an administrator, false otherwise */
    private boolean isAuthenticatedAsAdmin() {
        return getSecurityManager().checkAuthenticationForAdminRole();
    }

    /** @return the current authentication, or null if not authenticated */
    @Nullable
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** @return the GeoServer security manager */
    private GeoServerSecurityManager getSecurityManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }
}
