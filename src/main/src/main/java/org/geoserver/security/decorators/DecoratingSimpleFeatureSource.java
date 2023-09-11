/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.util.Set;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.FeatureListener;
import org.geotools.api.data.Query;
import org.geotools.api.data.QueryCapabilities;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.decorate.AbstractDecorator;

/**
 * Delegates every method to the wrapped simple feature source. Subclasses will override selected
 * methods to perform their "decoration" job
 *
 * @author Josh Vote, CSIRO Earth Science and Resource Engineering
 */
public abstract class DecoratingSimpleFeatureSource extends AbstractDecorator<SimpleFeatureSource>
        implements SimpleFeatureSource {

    public DecoratingSimpleFeatureSource(SimpleFeatureSource delegate) {
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
    public DataAccess<SimpleFeatureType, SimpleFeature> getDataStore() {
        return delegate.getDataStore();
    }

    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {
        return delegate.getFeatures();
    }

    @Override
    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return delegate.getFeatures(filter);
    }

    @Override
    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
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
    public SimpleFeatureType getSchema() {
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
