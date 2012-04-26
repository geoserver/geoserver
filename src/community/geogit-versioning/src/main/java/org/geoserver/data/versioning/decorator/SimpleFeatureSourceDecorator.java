package org.geoserver.data.versioning.decorator;

import java.io.IOException;

import org.geogit.api.RevTree;
import org.geogit.repository.Repository;
import org.geoserver.data.versioning.SimpleVersioningFeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

public class SimpleFeatureSourceDecorator extends
        FeatureSourceDecorator<SimpleFeatureType, SimpleFeature> implements
        SimpleVersioningFeatureSource {

    public SimpleFeatureSourceDecorator(SimpleFeatureSource unversioned, Repository repo) {
        super(unversioned, repo);
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
