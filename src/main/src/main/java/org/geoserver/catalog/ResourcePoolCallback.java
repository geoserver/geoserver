/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.catalog;

import org.geotools.data.FeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import java.io.Serializable;

/**
 * Basic Extension for {@link ResourcePool} class. Intended for rebuilding {@link FeatureType} and getting modified
 * features through wrapper {@link FeatureSource}
 *
 * @param <FT> feature type class
 * @param <F> feature class
 */
public interface ResourcePoolCallback<FT extends FeatureType, F extends Feature> extends Serializable {

    /**
     * Checks whether given feature type can be rebuild based on its source value.
     *
     * @param info feature layer info
     * @param featureType source feature type
     * @return true if callback can be applied
     */
    boolean canBuildFeatureType(FeatureTypeInfo info, FT featureType);

    /**
     * Rebuilds given feature type.
     * @param info feature layer info
     * @param featureType source feature type
     * @return new feature type definition
     */
    FT buildFeatureType(FeatureTypeInfo info, FT featureType);

    /**
     * Checks whether given feature source can be wrapped.
     *
     * @param info feature layer info
     * @param featureSource feature source to be wrapped
     * @return true if callback can be applied
     */
    boolean canWrapFeatureSource(FeatureTypeInfo info, FeatureSource<FT, F> featureSource);

    /**
     * Wraps given feature source into new implementation.
     *
     * @param info feature layer info
     * @param featureSource feature source to be wrapped
     * @return wrapping feature source
     */
    FeatureSource<FT, F> wrapFeatureSource(FeatureTypeInfo info, FeatureSource<FT, F> featureSource);
}
