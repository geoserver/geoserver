/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.FeatureVisitor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;

/**
 * Simple interface allowing access to the original main {@link FeatureTypeInfo} behind a feature collection. Has a
 * simple and a complex implementation.
 */
public interface TypeInfoCollectionWrapper {

    @SuppressWarnings("unchecked")
    static <T extends FeatureType, F extends Feature> FeatureCollection<T, F> wrap(
            FeatureCollection<T, F> delegate, FeatureTypeInfo featureTypeInfo) {
        if (delegate instanceof SimpleFeatureCollection collection) {
            return (FeatureCollection<T, F>) new Simple(collection, featureTypeInfo);
        } else {
            return new Complex<>(delegate, featureTypeInfo);
        }
    }

    class Complex<T extends FeatureType, F extends Feature> extends DecoratingFeatureCollection<T, F>
            implements TypeInfoCollectionWrapper {

        private final FeatureTypeInfo featureTypeInfo;

        protected Complex(FeatureCollection<T, F> delegate, FeatureTypeInfo featureTypeInfo) {
            super(delegate);
            this.featureTypeInfo = featureTypeInfo;
        }

        @Override
        public FeatureTypeInfo getFeatureTypeInfo() {
            return featureTypeInfo;
        }

        @Override
        protected boolean canDelegate(FeatureVisitor visitor) {
            return true;
        }
    }

    class Simple extends DecoratingSimpleFeatureCollection implements TypeInfoCollectionWrapper {

        private final FeatureTypeInfo featureTypeInfo;

        protected Simple(SimpleFeatureCollection delegate, FeatureTypeInfo featureTypeInfo) {
            super(delegate);
            this.featureTypeInfo = featureTypeInfo;
        }

        @Override
        public FeatureTypeInfo getFeatureTypeInfo() {
            return featureTypeInfo;
        }

        @Override
        protected boolean canDelegate(FeatureVisitor visitor) {
            return true;
        }
    }

    /** Returns the feature information */
    FeatureTypeInfo getFeatureTypeInfo();
}
