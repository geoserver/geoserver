/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;

/**
 * A secured implementation of {@link Resource} that delegates to an underlying resource while enforcing access control
 * through a {@link SecureResourceStore}.
 *
 * <p>Navigation methods ({@link #parent()}, {@link #get(String)}, {@link #list()}) wrap returned resources as
 * {@code SecuredResource} instances to maintain security throughout the resource hierarchy. I/O methods delegate to the
 * store for access checks.
 */
class SecuredResource implements Resource {

    /** The underlying resource being secured */
    final Resource delegate;

    /** The resource store responsible for enforcing security */
    private final SecureResourceStore store;

    /**
     * Creates a new secured resource.
     *
     * @param delegate the underlying resource to secure
     * @param store the security enforcement store
     */
    public SecuredResource(Resource delegate, SecureResourceStore store) {
        Objects.requireNonNull(delegate);
        Objects.requireNonNull(store);
        this.delegate = delegate;
        this.store = store;
    }

    @Override
    public String path() {
        return delegate.path();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    /**
     * Returns the type of this resource, or {@link Type#UNDEFINED} if the current user does not have read access to the
     * resource.
     *
     * @return the resource type, or UNDEFINED if access is denied
     */
    @Override
    public Type getType() {
        return store.getType(this);
    }

    /**
     * Lists child resources the current user has access to, wrapped as {@code SecuredResource} instances.
     *
     * @return a list of accessible child resources
     */
    @Override
    public List<Resource> list() {
        return store.list(path());
    }

    /**
     * Deletes this resource if the current user has write access.
     *
     * @return true if the resource was deleted, false otherwise
     */
    @Override
    public boolean delete() {
        return store.remove(path());
    }

    /**
     * Renames this resource if the current user has write access to both the source and destination.
     *
     * @param dest the destination resource
     * @return true if the resource was renamed, false otherwise
     */
    @Override
    public boolean renameTo(Resource dest) {
        return store.move(path(), dest.path());
    }

    /**
     * Opens an input stream to read this resource if the current user has read access.
     *
     * @return an input stream to the resource
     * @throws org.geoserver.rest.RestException 404 if the user doesn't have read access
     */
    @Override
    public InputStream in() {
        return store.in(this);
    }

    /**
     * Opens an output stream to write to this resource if the current user has write access.
     *
     * @return an output stream to the resource
     * @throws org.geoserver.rest.RestException 403 if the resource is read-only, 404 if not visible
     */
    @Override
    public OutputStream out() {
        return store.out(this);
    }

    /**
     * Returns the file handle for this resource if the current user has write access.
     *
     * @return the file for this resource
     * @throws org.geoserver.rest.RestException 403 if the resource is read-only, 404 if not visible
     */
    @Override
    public File file() {
        return store.file(this);
    }

    /**
     * Returns the directory handle for this resource if the current user has write access.
     *
     * @return the directory for this resource
     * @throws org.geoserver.rest.RestException 403 if the resource is read-only, 404 if not visible
     */
    @Override
    public File dir() {
        return store.dir(this);
    }

    @Override
    public long lastmodified() {
        return delegate.lastmodified();
    }

    /** Returns the parent resource, wrapped as a {@code SecuredResource}. */
    @Override
    public Resource parent() {
        return store.wrap(delegate.parent());
    }

    /** Returns a child resource, wrapped as a {@code SecuredResource}. */
    @Override
    public Resource get(String childPath) {
        return store.wrap(delegate.get(childPath));
    }

    @Override
    public Lock lock() {
        return delegate.lock();
    }

    @Override
    public void addListener(ResourceListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(ResourceListener listener) {
        delegate.removeListener(listener);
    }
}
