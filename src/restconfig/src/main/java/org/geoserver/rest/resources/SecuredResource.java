/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.geoserver.platform.resource.Resource;

/**
 * A secured implementation of {@link Resource} that delegates security decisions to a {@link SecureResourceStore}.
 *
 * <p>This class extends {@link ResourceDecorator} and overrides methods to enforce security checks before accessing or
 * modifying resources. It uses the associated {@link SecureResourceStore} to determine if the current user has
 * appropriate permissions for each operation.
 */
class SecuredResource extends ResourceDecorator {

    /** The resource store responsible for enforcing security */
    private final SecureResourceStore store;

    /**
     * Creates a new secured resource.
     *
     * @param delegate the underlying resource to secure
     * @param store the security enforcement store
     */
    public SecuredResource(Resource delegate, SecureResourceStore store) {
        super(delegate, store::wrap);
        this.store = store;
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
     * Lists the child resources that the current user has access to.
     *
     * @return a list of accessible child resources
     */
    @Override
    protected List<Resource> listInternal() {
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
     * @throws IllegalStateException if the user doesn't have read access
     */
    @Override
    public InputStream in() {
        return store.in(this);
    }

    /**
     * Opens an output stream to write to this resource if the current user has write access.
     *
     * @return an output stream to the resource
     * @throws IllegalStateException if the user doesn't have write access
     */
    @Override
    public OutputStream out() {
        return store.out(this);
    }

    @Override
    public File file() {
        return delegate.file();
    }

    @Override
    public File dir() {
        return delegate.dir();
    }

    @Override
    public long lastmodified() {
        return delegate.lastmodified();
    }
}
