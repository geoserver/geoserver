/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.opengis.feature.FeatureVisitor;

/**
 * Simple interface allowing access to the original main {@link FeatureTypeInfo} behind a feature
 * collection. Has a simple and a complex implementation.
 */
public interface TypeInfoCollectionWrapper {

    static FeatureCollection wrap(FeatureCollection delegate, FeatureTypeInfo featureTypeInfo) {
        if (delegate instanceof SimpleFeatureCollection) {
            return new Simple((SimpleFeatureCollection) delegate, featureTypeInfo);
        } else {
            return new Complex(delegate, featureTypeInfo);
        }
    }

    class Complex extends DecoratingFeatureCollection implements TypeInfoCollectionWrapper {

        private final FeatureTypeInfo featureTypeInfo;

        protected Complex(FeatureCollection delegate, FeatureTypeInfo featureTypeInfo) {
            super(delegate);
            this.featureTypeInfo = featureTypeInfo;
        }

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
