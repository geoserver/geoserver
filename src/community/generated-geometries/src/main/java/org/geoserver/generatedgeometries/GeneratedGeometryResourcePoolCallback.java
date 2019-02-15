/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.generatedgeometries;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePoolCallback;
import org.geoserver.generatedgeometries.longitudelatitude.LongLatFeatureSource;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.vfny.geoserver.global.ConfigurationException;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.geoserver.generatedgeometries.GeometryGenerationStrategy.getStrategyName;

public class GeneratedGeometryResourcePoolCallback implements ResourcePoolCallback<SimpleFeatureType, SimpleFeature> {

    private static final long serialVersionUID = 1L;
    
    private GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> strategy;

    void setStrategy(GeometryGenerationStrategy<SimpleFeatureType, SimpleFeature> strategy) {
        this.strategy = strategy;
    }

    @Override
    public boolean canBuildFeatureType(FeatureTypeInfo info, SimpleFeatureType featureType) {
        String strategyName = getStrategyName(info);
        return getStrategy(strategyName) != null;
    }

    private GeometryGenerationStrategy getStrategy(String strategyName) {
        if (strategy == null && isNotEmpty(strategyName)) {
            strategy = GeoServerExtensions.extensions(GeometryGenerationStrategy.class)
                    .stream()
                    .filter(stg -> stg.getName().equals(strategyName))
                    .findFirst()
                    .orElse(null);
        }
        return strategy;
    }

    @Override
    public SimpleFeatureType buildFeatureType(FeatureTypeInfo info, SimpleFeatureType featureType) {
        if (canBuildFeatureType(info, featureType)) {
            try {
                return strategy.defineGeometryAttributeFor(info, featureType);
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
        }
        return featureType;
    }

    @Override
    public boolean canWrapFeatureSource(FeatureTypeInfo info, FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
        return canBuildFeatureType(info, null);
    }

    @Override
    public FeatureSource<SimpleFeatureType, SimpleFeature> wrapFeatureSource(FeatureTypeInfo info, FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
        return new LongLatFeatureSource(info, (SimpleFeatureSource) featureSource, strategy);
    }

}
