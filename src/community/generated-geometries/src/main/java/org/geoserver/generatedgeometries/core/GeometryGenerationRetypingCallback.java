/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.core;

import static java.lang.String.format;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.RetypeFeatureTypeCallback;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

public class GeometryGenerationRetypingCallback implements RetypeFeatureTypeCallback {

    private static Logger LOGGER =
            Logging.getLogger(GeometryGenerationRetypingCallback.class.getPackage().getName());

    private final GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> strategy;

    public GeometryGenerationRetypingCallback(
            GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> strategy) {
        this.strategy = strategy;
    }

    private boolean canHandleFeatureType(FeatureTypeInfo featureTypeInfo) {
        return strategy.canHandle(featureTypeInfo, null);
    }

    @Override
    public FeatureType retypeFeatureType(FeatureTypeInfo featureTypeInfo, FeatureType featureType) {
        if (canHandleFeatureType(featureTypeInfo)) {
            try {
                return strategy.defineGeometryAttributeFor(
                        featureTypeInfo, (SimpleFeatureType) featureType);
            } catch (GeneratedGeometryConfigurationException e) {
                LOGGER.log(Level.WARNING, format("cannot build feature type [%s]", featureType), e);
            }
        }
        return featureType;
    }

    @Override
    public <T extends FeatureType, U extends Feature> FeatureSource<T, U> wrapFeatureSource(
            FeatureTypeInfo featureTypeInfo, FeatureSource<T, U> featureSource) {
        if (canHandleFeatureType(featureTypeInfo)) {
            return (FeatureSource<T, U>)
                    new GeometryGenerationFeatureSource(
                            featureTypeInfo, (SimpleFeatureSource) featureSource, strategy);
        }
        return featureSource;
    }
}
