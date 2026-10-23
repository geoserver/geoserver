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
 * {@link Resource} decorator that routes every access-controlled operation through its {@link SecureResourceStore}.
 *
 * <p>Navigation methods ({@link #parent()}, {@link #get(String)}, {@link #list()}) wrap returned resources as
 * {@code SecuredResource} instances to keep the whole hierarchy secured. {@link #getType()} reports
 * {@link Type#UNDEFINED} for resources the user can't read, making them appear non-existent.
 */
class SecuredResource implements Resource {

    final Resource delegate;

    private final SecureResourceStore store;

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

    @Override
    public Type getType() {
        return store.getType(this);
    }

    @Override
    public List<Resource> list() {
        return store.list(path());
    }

    @Override
    public boolean delete() {
        return store.remove(path());
    }

    @Override
    public boolean renameTo(Resource dest) {
        return store.move(path(), dest.path());
    }

    @Override
    public InputStream in() {
        return store.in(this);
    }

    @Override
    public OutputStream out() {
        return store.out(this);
    }

    @Override
    public File file() {
        return store.file(this);
    }

    @Override
    public File dir() {
        return store.dir(this);
    }

    @Override
    public long lastmodified() {
        return delegate.lastmodified();
    }

    @Override
    public Resource parent() {
        return store.wrap(delegate.parent());
    }

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
