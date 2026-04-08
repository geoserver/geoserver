/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Set;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.FeatureListener;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.data.QueryCapabilities;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * A FeatureSource that applies a filter to the delegate FeatureSource. The implementation is enough to be used in the
 * OpenSearch for EO context, where we need to wrap an existing FeatureSource applying a filter, but it's not ironclad
 * enough to be used in all contexts.
 */
class FilteredFeatureSource implements FeatureSource<FeatureType, Feature> {

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    FeatureSource<FeatureType, Feature> delegate;
    Filter securityFilter;

    public FilteredFeatureSource(FeatureSource<FeatureType, Feature> delegate, Filter filter) {
        this.delegate = delegate;
        this.securityFilter = filter;
    }

    private Filter restrictFilter(Filter f1) {
        if (f1 == null || f1 == Filter.INCLUDE) {
            return securityFilter;
        }
        return FF.and(f1, securityFilter);
    }

    private Query restrictQuery(Query query) {
        Filter mixedFilter = restrictFilter(query.getFilter());
        Query newQuery = new Query(query);
        newQuery.setFilter(mixedFilter);
        return newQuery;
    }

    @Override
    public Name getName() {
        return delegate.getName();
    }

    @Override
    public ResourceInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public DataAccess<FeatureType, Feature> getDataStore() {
        return delegate.getDataStore();
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return delegate.getQueryCapabilities();
    }

    @Override
    public void addFeatureListener(FeatureListener listener) {
        delegate.addFeatureListener(listener);
    }

    @Override
    public void removeFeatureListener(FeatureListener listener) {
        delegate.removeFeatureListener(listener);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Filter filter) throws IOException {
        Filter mixedFilter = restrictFilter(filter);
        return delegate.getFeatures(mixedFilter);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Query query) throws IOException {
        Query newQuery = restrictQuery(query);
        return delegate.getFeatures(newQuery);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures() throws IOException {
        return delegate.getFeatures(securityFilter);
    }

    @Override
    public FeatureType getSchema() {
        return delegate.getSchema();
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        Query query = new Query();
        query.setFilter(securityFilter);
        return delegate.getBounds(query);
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        Query newQuery = restrictQuery(query);
        return delegate.getBounds(newQuery);
    }

    @Override
    public int getCount(Query query) throws IOException {
        Query securedQuery = restrictQuery(query);
        return delegate.getCount(securedQuery);
    }

    @Override
    public Set<RenderingHints.Key> getSupportedHints() {
        return delegate.getSupportedHints();
    }
}
