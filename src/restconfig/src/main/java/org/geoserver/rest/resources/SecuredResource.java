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

class SecuredResource extends ResourceDecorator {

    private final SecureResourceStore store;

    public SecuredResource(Resource delegate, SecureResourceStore store) {
        super(delegate, store::wrap);
        this.store = store;
    }

    @Override
    public Type getType() {
        return store.getType(this);
    }

    @Override
    protected List<Resource> listInternal() {
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
