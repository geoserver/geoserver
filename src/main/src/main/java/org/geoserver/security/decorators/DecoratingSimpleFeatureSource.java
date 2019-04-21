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
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.decorate.AbstractDecorator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

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

    public void addFeatureListener(FeatureListener listener) {
        delegate.addFeatureListener(listener);
    }

    public ReferencedEnvelope getBounds() throws IOException {
        return delegate.getBounds();
    }

    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return delegate.getBounds(query);
    }

    public int getCount(Query query) throws IOException {
        return delegate.getCount(query);
    }

    public DataAccess<SimpleFeatureType, SimpleFeature> getDataStore() {
        return delegate.getDataStore();
    }

    public SimpleFeatureCollection getFeatures() throws IOException {
        return delegate.getFeatures();
    }

    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return delegate.getFeatures(filter);
    }

    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        return delegate.getFeatures(query);
    }

    public ResourceInfo getInfo() {
        return delegate.getInfo();
    }

    public Name getName() {
        return delegate.getName();
    }

    public QueryCapabilities getQueryCapabilities() {
        return delegate.getQueryCapabilities();
    }

    public SimpleFeatureType getSchema() {
        return delegate.getSchema();
    }

    public Set<Key> getSupportedHints() {
        return delegate.getSupportedHints();
    }

    public void removeFeatureListener(FeatureListener listener) {
        delegate.removeFeatureListener(listener);
    }
}
