/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.longitudelatitude;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourcePoolCallback;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.vfny.geoserver.global.ConfigurationException;

import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

public class LongLatGeometryResourcePoolCallback
        implements ResourcePoolCallback<SimpleFeatureType, SimpleFeature> {

    private static Logger LOGGER =
            Logging.getLogger(LongLatGeometryResourcePoolCallback.class.getPackage().getName());

    private static final long serialVersionUID = 1L;

    private final LongLatGeometryGenerationStrategy strategy;

    public LongLatGeometryResourcePoolCallback(LongLatGeometryGenerationStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * {@inheritDoc}
     *
     * @param info feature layer info
     * @param unused this implementation does not need it
     * @return true if supporting strategy can handle given info or false otherwise
     */
    @Override
    public boolean canBuildFeatureType(FeatureTypeInfo info, SimpleFeatureType unused) {
        return strategy.canHandle(info);
    }

    @Override
    public SimpleFeatureType buildFeatureType(FeatureTypeInfo info, SimpleFeatureType featureType) {
        if (canBuildFeatureType(info, featureType)) {
            try {
                return strategy.defineGeometryAttributeFor(info, featureType);
            } catch (ConfigurationException e) {
                LOGGER.log(Level.WARNING, format("cannot build feature type [%s]", featureType), e);
            }
        }
        return featureType;
    }

    @Override
    public boolean canWrapFeatureSource(
            FeatureTypeInfo info, FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
        return canBuildFeatureType(info, null);
    }

    @Override
    public FeatureSource<SimpleFeatureType, SimpleFeature> wrapFeatureSource(
            FeatureTypeInfo info, FeatureSource<SimpleFeatureType, SimpleFeature> featureSource) {
        if (canWrapFeatureSource(info, null)) {
            return new LongLatFeatureSource(info, (SimpleFeatureSource) featureSource, strategy);
        }
        return featureSource;
    }
}
