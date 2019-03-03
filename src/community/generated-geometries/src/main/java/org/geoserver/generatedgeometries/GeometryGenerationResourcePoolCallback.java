/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries;

import static java.lang.String.format;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.RetypeFeatureTypeCallback;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.vfny.geoserver.global.ConfigurationException;

public class GeometryGenerationResourcePoolCallback
        implements RetypeFeatureTypeCallback<SimpleFeatureType, SimpleFeature> {

    private static Logger LOGGER =
            Logging.getLogger(GeometryGenerationResourcePoolCallback.class.getPackage().getName());

    private static final long serialVersionUID = 1L;

    private final GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> strategy;

    public GeometryGenerationResourcePoolCallback(
            GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> strategy) {
        this.strategy = strategy;
    }

    private boolean canHandleFeatureType(
            FeatureTypeInfo featureTypeInfo, SimpleFeatureType simpleFeatureType) {
        return strategy.canHandle(featureTypeInfo, simpleFeatureType);
    }

    @Override
    public SimpleFeatureType retypeFeatureType(
            FeatureTypeInfo featureTypeInfo, SimpleFeatureType featureType) {
        if (canHandleFeatureType(featureTypeInfo, featureType)) {
            try {
                return strategy.defineGeometryAttributeFor(featureTypeInfo, featureType);
            } catch (ConfigurationException e) {
                LOGGER.log(Level.WARNING, format("cannot build feature type [%s]", featureType), e);
            }
        }
        return featureType;
    }

    private boolean canWrapFeatureSource(
            FeatureTypeInfo info, FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
        return canHandleFeatureType(info, null);
    }

    @Override
    public FeatureSource<SimpleFeatureType, SimpleFeature> wrapFeatureSource(
            FeatureTypeInfo featureTypeInfo,
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
        if (canWrapFeatureSource(featureTypeInfo, null)) {
            return new GeometryGenerationFeatureSource(
                    featureTypeInfo, (SimpleFeatureSource) featureSource, strategy);
        }
        return featureSource;
    }
}
