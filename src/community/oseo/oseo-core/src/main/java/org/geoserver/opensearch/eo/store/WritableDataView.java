/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.List;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.data.DataUtilities;
import org.geotools.data.view.DefaultView;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;

/**
 * Writable subclass of DefaultView. It's not completely correct, but good enough for the specific case at hand
 *
 * @author Andrea Aime - GeoSolutions
 */
class WritableDataView extends DefaultView implements SimpleFeatureStore {

    protected SimpleFeatureStore delegate;
    protected Query query;

    public WritableDataView(SimpleFeatureStore store, Query query) throws SchemaException {
        super(store, query);
        this.delegate = store;
        this.query = query;
    }

    @Override
    public List<FeatureId> addFeatures(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection)
            throws IOException {
        return delegate.addFeatures(featureCollection);
    }

    @Override
    public void removeFeatures(Filter filter) throws IOException {
        Filter mixedFilter = mixFilter(filter);
        delegate.removeFeatures(mixedFilter);
    }

    private Filter mixFilter(Filter filter) {
        Query query = new Query();
        query.setFilter(filter);
        Query mixed = DataUtilities.mixQueries(this.query, query, null);
        Filter mixedFilter = mixed.getFilter();
        return mixedFilter;
    }

    @Override
    public void modifyFeatures(Name[] attributeNames, Object[] attributeValues, Filter filter) throws IOException {
        Filter mixedFilter = mixFilter(filter);
        delegate.modifyFeatures(attributeNames, attributeValues, mixedFilter);
    }

    @Override
    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter) throws IOException {
        Filter mixedFilter = mixFilter(filter);
        delegate.modifyFeatures(attributeName, attributeValue, mixedFilter);
    }

    @Override
    public void setFeatures(FeatureReader<SimpleFeatureType, SimpleFeature> reader) throws IOException {
        // need to overwrite only the features in this view
        removeFeatures(Filter.INCLUDE);
        addFeatures(DataUtilities.collection(reader));
    }

    @Override
    public void setTransaction(Transaction transaction) {
        delegate.setTransaction(transaction);
    }

    @Override
    public Transaction getTransaction() {
        return delegate.getTransaction();
    }

    @Override
    public void modifyFeatures(String name, Object attributeValue, Filter filter) throws IOException {
        Filter mixedFilter = mixFilter(filter);
        delegate.modifyFeatures(name, attributeValue, mixedFilter);
    }

    @Override
    public void modifyFeatures(String[] names, Object[] attributeValues, Filter filter) throws IOException {
        Filter mixedFilter = mixFilter(filter);
        delegate.modifyFeatures(names, attributeValues, mixedFilter);
    }
}
