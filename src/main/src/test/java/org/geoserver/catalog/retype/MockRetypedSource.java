/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.retype;

import java.io.IOException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.decorators.DecoratingSimpleFeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

public class MockRetypedSource extends DecoratingSimpleFeatureSource {

    private final FeatureTypeInfo featureTypeInfo;
    private SimpleFeatureType cachedFeatureType;
    SimpleFeatureSource delegate;

    RetypeHelper converter = new RetypeHelper();

    public MockRetypedSource(FeatureTypeInfo featureTypeInfo, SimpleFeatureSource delegate) {
        super(delegate);
        this.featureTypeInfo = featureTypeInfo;

        this.delegate = delegate;
    }

    @Override
    public SimpleFeatureType getSchema() {

        SimpleFeatureType src = super.getSchema();
        try {
            return converter.defineGeometryAttributeFor(featureTypeInfo, src);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return src;
    }

    @Override
    public SimpleFeatureCollection getFeatures() throws IOException {

        SimpleFeatureCollection features = getFeatures(Query.ALL);
        return new MockRetypedFeatureCollection(features, featureTypeInfo, getSchema());
    }

    @Override
    public SimpleFeatureCollection getFeatures(Filter srcFilter) throws IOException {
        Query query = new Query(Query.ALL);
        query.setFilter(srcFilter);
        Query newQuery = converter.convertQuery(featureTypeInfo, query);
        SimpleFeatureCollection features = super.getFeatures(newQuery);
        return new MockRetypedFeatureCollection(features, featureTypeInfo, getSchema());
    }

    @Override
    public SimpleFeatureCollection getFeatures(Query srcQuery) throws IOException {
        Query newQuery = converter.convertQuery(featureTypeInfo, srcQuery);
        SimpleFeatureCollection features = super.getFeatures(newQuery);
        return new MockRetypedFeatureCollection(features, featureTypeInfo, getSchema());
    }

    @Override
    public int getCount(Query srcQuery) throws IOException {
        Query newQuery = converter.convertQuery(featureTypeInfo, srcQuery);
        return super.getCount(newQuery);
    }
}
