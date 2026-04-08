/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * A FreeMarker {@link BeansWrapper} decorator that adds support for tracking and cleaning up {@link AutoCloseable}
 * resources used in templates.
 *
 * <p>This wrapper extends {@link BeansWrapper} and implements the Decorator pattern using FreeMarker's
 * {@code setOuterIdentity()} mechanism to intercept all wrapping operations. When {@link AutoCloseable} objects (such
 * as {@link org.geoserver.catalog.util.CloseableIterator} or streams) are returned from bean properties, this wrapper
 * tracks them for automatic cleanup after template rendering.
 *
 * <p>This keeps concerns separated: the delegate wrapper (e.g., {@link org.geoserver.template.FeatureWrapper}) handles
 * domain-specific objects like Features and FeatureCollections, while this decorator adds generic AutoCloseable
 * resource tracking.
 *
 * <p>Design: Implements the Decorator pattern. By calling {@code delegate.setOuterIdentity(this)}, we ensure that when
 * inner wrappers invoke {@code getOuterIdentity().wrap()}, they call this wrapper's {@code wrap()} method, allowing us
 * to intercept and track AutoCloseable resources.
 *
 * <p>This wrapper works with any {@link AutoCloseable} type, regardless of how the delegate wraps it for template use.
 */
class AutoCloseableTrackingWrapper extends BeansWrapper {

    private final BeansWrapper delegate;

    /**
     * Creates a wrapper that decorates the given delegate with AutoCloseable tracking.
     *
     * <p>This wrapper extends BeansWrapper and uses {@code setOuterIdentity()} to ensure all wrapping operations go
     * through this wrapper's {@code wrap()} method.
     *
     * @param delegate the underlying BeansWrapper to delegate to (typically a FeatureWrapper or SafeWrapper)
     */
    public AutoCloseableTrackingWrapper(BeansWrapper delegate) {
        super(delegate.getIncompatibleImprovements());
        this.delegate = delegate;
        // Set this wrapper as the outer identity of the delegate
        // This ensures delegate.getOuterIdentity() returns this wrapper
        delegate.setOuterIdentity(this);
    }

    @Override
    public TemplateModel wrap(Object object) throws TemplateModelException {
        // Track any AutoCloseable resource before wrapping
        if (object instanceof AutoCloseable) {
            AutoCloseableTracker.track((AutoCloseable) object);
        }

        // Delegate to the inner wrapper for actual wrapping
        // The delegate handles type-specific conversions (Iterator -> IteratorModel, Feature -> FeatureModel, etc.)
        return delegate.wrap(object);
    }
}
