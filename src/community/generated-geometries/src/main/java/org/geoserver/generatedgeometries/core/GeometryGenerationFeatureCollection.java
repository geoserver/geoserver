/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.core;

import java.util.NoSuchElementException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

class GeometryGenerationFeatureCollection extends DecoratingSimpleFeatureCollection {

    private final FeatureTypeInfo featureTypeInfo;
    private final SimpleFeatureType schema;
    private final GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> strategy;

    GeometryGenerationFeatureCollection(
            SimpleFeatureCollection delegate,
            FeatureTypeInfo featureTypeInfo,
            SimpleFeatureType schema,
            GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> strategy) {
        super(delegate);

        this.featureTypeInfo = featureTypeInfo;
        this.schema = schema;
        this.strategy = strategy;
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
            return strategy.generateGeometry(featureTypeInfo, schema, feature);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
