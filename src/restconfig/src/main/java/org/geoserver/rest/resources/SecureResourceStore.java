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
 * {@link ResourceStore} decorator that limits what workspace administrators can see and modify, using
 * {@link WorkspaceAdminResourceFilter} to map resource paths to access levels. Full administrators bypass the checks
 * entirely.
 *
 * <p>The {@link ResourceStore#get(String)} contract requires returning a {@link Resource} of type
 * {@link Resource.Type#UNDEFINED} for non-existent paths rather than throwing an exception. Because of this,
 * {@link #get(String)} cannot reject inaccessible paths upfront: it returns a {@link SecuredResource} that defers the
 * access check to the actual operation ({@code in()}, {@code out()}, {@code file()}, {@code dir()}), which throws a
 * {@link RestException} with status 404 if the resource is not visible to the user, or 403 if it is visible but
 * read-only.
 *
 * @see ResourceController
 */
class SecureResourceStore implements ResourceStore {

    private final ResourceStore delegate;

    private final WorkspaceAdminResourceFilter resourcePathFilter;

    SecureResourceStore(ResourceStore delegate) {
        this.delegate = delegate;
        WorkspaceAdminAuthorizer workspaceAdminAuthorizer =
                WorkspaceAdminAuthorizer.get().orElseThrow();
        this.resourcePathFilter = new WorkspaceAdminResourceFilter(workspaceAdminAuthorizer);
    }

    // ResourceStore methods //

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

    // support methods for SecuredResource so all logic is centralized in the store //

    /** Lists the children of {@code path} the current user can read, or an empty list if the path itself isn't. */
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

    /** Returns {@link Type#UNDEFINED} if the current user can't read the resource, making it appear non-existent. */
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
        throw notFound(resource);
    }

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

    SecuredResource wrap(Resource resource) {
        if (resource instanceof SecuredResource secured) {
            return secured;
        }
        return new SecuredResource(resource, this);
    }

    private boolean canRead(Resource resource) {
        return canRead(resource.path());
    }

    private boolean canWrite(Resource resource) {
        return canWrite(resource.path());
    }

    boolean canRead(String path) {
        boolean isFullAdmin = isAuthenticatedAsAdmin();
        return isFullAdmin || userCanRead(path);
    }

    boolean canWrite(String path) {
        boolean isFullAdmin = isAuthenticatedAsAdmin();
        return isFullAdmin || userCanWriteTo(path);
    }

    private boolean userCanRead(String path) {
        ResourceAccess access = getAccess(path);
        return access.canRead();
    }

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

    private boolean isAuthenticatedAsAdmin() {
        return getSecurityManager().checkAuthenticationForAdminRole();
    }

    @Nullable
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private GeoServerSecurityManager getSecurityManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }
}
