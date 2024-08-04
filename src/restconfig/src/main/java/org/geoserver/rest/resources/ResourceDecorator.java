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
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;

class ResourceDecorator implements Resource {

    protected static final UnaryOperator<Resource> IDENTITY = UnaryOperator.identity();

    protected final Resource delegate;

    protected final UnaryOperator<Resource> builder;

    public ResourceDecorator(Resource delegate) {
        this(delegate, IDENTITY);
    }

    public ResourceDecorator(Resource delegate, UnaryOperator<Resource> builder) {
        Objects.requireNonNull(delegate);
        Objects.requireNonNull(builder);
        this.delegate = delegate;
        this.builder = builder;
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

    @Override
    public InputStream in() {
        return delegate.in();
    }

    @Override
    public OutputStream out() {
        return delegate.out();
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

    @Override
    public Resource parent() {
        return builder.apply(delegate.parent());
    }

    @Override
    public Resource get(String resourcePath) {
        return builder.apply(delegate.get(resourcePath));
    }

    @Override
    public final List<Resource> list() {
        List<Resource> list = listInternal();
        if (IDENTITY != builder) {
            list = list.stream().map(builder).collect(Collectors.toList());
        }
        return list;
    }

    protected List<Resource> listInternal() {
        return delegate.list();
    }

    @Override
    public Type getType() {
        return delegate.getType();
    }

    @Override
    public boolean delete() {
        return delegate.delete();
    }

    @Override
    public boolean renameTo(Resource dest) {
        return delegate.renameTo(dest);
    }
}
