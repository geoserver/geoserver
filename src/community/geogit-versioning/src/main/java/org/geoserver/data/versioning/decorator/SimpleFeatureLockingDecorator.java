package org.geoserver.data.versioning.decorator;

import java.io.IOException;

import org.geogit.api.RevTree;
import org.geogit.repository.Repository;
import org.geoserver.data.versioning.SimpleVersioningFeatureLocking;
import org.geotools.data.FeatureLocking;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

public class SimpleFeatureLockingDecorator extends
        FeatureLockingDecorator<SimpleFeatureType, SimpleFeature> implements
        SimpleVersioningFeatureLocking {

    SimpleFeatureStoreDecorator storeDelegate;

    public SimpleFeatureLockingDecorator(
            FeatureLocking<SimpleFeatureType, SimpleFeature> unversioned, Repository repo) {
        super(unversioned, repo);
        storeDelegate = new SimpleFeatureStoreDecorator((SimpleFeatureStore) unversioned, repo);
    }

    @Override
    public void modifyFeatures(String name, Object attributeValue, Filter filter)
            throws IOException {
        storeDelegate.modifyFeatures(name, attributeValue, filter);
    }

    @Override
    public void modifyFeatures(String[] names, Object[] attributeValues, Filter filter)
            throws IOException {
        storeDelegate.modifyFeatures(names, attributeValues, filter);
    }

    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {
        return (SimpleFeatureCollection) super.getFeatures();
    }

    @Override
    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return (SimpleFeatureCollection) super.getFeatures(filter);
    }

    @Override
    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        return (SimpleFeatureCollection) super.getFeatures(query);
    }

    @Override
    protected FeatureCollection<SimpleFeatureType, SimpleFeature> createFeatureCollection(
            FeatureCollection<SimpleFeatureType, SimpleFeature> delegate, RevTree typeTree) {
        return new SimpleResourceIdAssigningFeatureCollection((SimpleFeatureCollection) delegate,
                this, typeTree);
    }
}
