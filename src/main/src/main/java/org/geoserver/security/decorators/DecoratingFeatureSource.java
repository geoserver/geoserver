/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.util.Set;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * Delegates every method to the wrapped feature source. Subclasses will override selected methods
 * to perform their "decoration" job
 *
 * @author Andrea Aime
 */
public abstract class DecoratingFeatureSource<T extends FeatureType, F extends Feature>
        extends AbstractDecorator<FeatureSource<T, F>> implements FeatureSource<T, F> {

    public DecoratingFeatureSource(FeatureSource<T, F> delegate) {
        super(delegate);
    }

    @Override
    public void addFeatureListener(FeatureListener listener) {
        delegate.addFeatureListener(listener);
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return delegate.getBounds();
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return delegate.getBounds(query);
    }

    @Override
    public int getCount(Query query) throws IOException {
        return delegate.getCount(query);
    }

    @Override
    public DataAccess<T, F> getDataStore() {
        return delegate.getDataStore();
    }

    @Override
    public FeatureCollection<T, F> getFeatures() throws IOException {
        return delegate.getFeatures();
    }

    @Override
    public FeatureCollection<T, F> getFeatures(Filter filter) throws IOException {
        return delegate.getFeatures(filter);
    }

    @Override
    public FeatureCollection<T, F> getFeatures(Query query) throws IOException {
        return delegate.getFeatures(query);
    }

    @Override
    public ResourceInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public Name getName() {
        return delegate.getName();
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return delegate.getQueryCapabilities();
    }

    @Override
    public T getSchema() {
        return delegate.getSchema();
    }

    @Override
    public Set<Key> getSupportedHints() {
        return delegate.getSupportedHints();
    }

    @Override
    public void removeFeatureListener(FeatureListener listener) {
        delegate.removeFeatureListener(listener);
    }
}
