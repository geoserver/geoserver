/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.retype;

import java.util.NoSuchElementException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class MockRetypedFeatureCollection extends DecoratingSimpleFeatureCollection {

    private final FeatureTypeInfo featureTypeInfo;
    private final SimpleFeatureType schema;

    MockRetypedFeatureCollection(
            SimpleFeatureCollection delegate,
            FeatureTypeInfo featureTypeInfo,
            SimpleFeatureType schema) {
        super(delegate);

        this.featureTypeInfo = featureTypeInfo;
        this.schema = schema;
    }

    @Override
    public SimpleFeatureType getSchema() {
        return this.schema;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new GeometryGenerationCollectionIterator(super.features());
    }

    private class GeometryGenerationCollectionIterator implements SimpleFeatureIterator {

        private final SimpleFeatureIterator delegate;

        RetypeHelper converter = new RetypeHelper();

        private GeometryGenerationCollectionIterator(SimpleFeatureIterator delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature feature = delegate.next();
            return converter.generateGeometry(featureTypeInfo, schema, feature);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
