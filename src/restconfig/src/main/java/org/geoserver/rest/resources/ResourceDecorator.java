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

/**
 * A decorator for {@link Resource} that delegates calls to an underlying resource while supporting customization
 * through a resource builder function.
 *
 * <p>This class serves as a base for creating specialized resource implementations where most behavior is delegated to
 * the underlying resource, but certain operations may be intercepted or modified.
 */
class ResourceDecorator implements Resource {

    /** Identity operator that returns the resource unchanged */
    protected static final UnaryOperator<Resource> IDENTITY = UnaryOperator.identity();

    /** The underlying resource being decorated */
    protected final Resource delegate;

    /**
     * Function that transforms resources when navigating the resource hierarchy (e.g., when calling {@link #parent()}
     * or {@link #get(String)})
     */
    protected final UnaryOperator<Resource> builder;

    /**
     * Creates a new resource decorator with the identity builder function.
     *
     * @param delegate the underlying resource to delegate to
     */
    public ResourceDecorator(Resource delegate) {
        this(delegate, IDENTITY);
    }

    /**
     * Creates a new resource decorator with the specified builder function.
     *
     * @param delegate the underlying resource to delegate to
     * @param builder the function to apply when creating new resources in the hierarchy
     * @throws NullPointerException if delegate or builder is null
     */
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

    /**
     * Returns the parent resource, applying the builder function to it.
     *
     * @return the decorated parent resource
     */
    @Override
    public Resource parent() {
        return builder.apply(delegate.parent());
    }

    /**
     * Returns a child resource with the given path, applying the builder function to it.
     *
     * @param resourcePath the path to the child resource
     * @return the decorated child resource
     */
    @Override
    public Resource get(String resourcePath) {
        return builder.apply(delegate.get(resourcePath));
    }

    /**
     * Lists all child resources, applying the builder function to each one. This method is final and delegates to
     * {@link #listInternal()} to allow subclasses to customize the listing behavior while ensuring the builder is
     * always applied.
     *
     * @return a list of decorated child resources
     */
    @Override
    public final List<Resource> list() {
        List<Resource> list = listInternal();
        if (IDENTITY != builder) {
            list = list.stream().map(this::decorate).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return list;
    }

    private Resource decorate(Resource resource) {
        return builder.apply(resource);
    }

    /**
     * Internal method to list child resources that can be overridden by subclasses. The default implementation simply
     * delegates to the underlying resource.
     *
     * @return a list of child resources before the builder is applied
     */
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
