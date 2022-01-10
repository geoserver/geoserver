/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import org.geotools.data.FeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * Extension point for {@link ResourcePool} allowing us to retype an existing {@link FeatureType},
 * and to rearrange the features produced by the correspondent {@link FeatureSource} by wrapping it.
 * This extension point can be, for example, used to add a new attribute or remove an existing one.
 * If the intend is instead to create a new feature type, the existing {@link FeatureTypeCallback}
 * extension point should be used.
 */
public interface RetypeFeatureTypeCallback {

    /**
     * Gives a chance to this callback to retype the provided feature type, if this callback has no
     * interest in retyping the provided feature type, then the unchanged provided feature type
     * should be returned. NULL should never be returned.
     *
     * @param featureTypeInfo non NULL GeoServer feature type info
     * @param featureType non NULL GeoTools data source feature type
     * @return retyped feature type or the unchanged provided feature type
     */
    default FeatureType retypeFeatureType(
            FeatureTypeInfo featureTypeInfo, FeatureType featureType) {
        return featureType;
    }

    /**
     * Gives a chance to this callback to wrap the provided feature source, if this callback has no
     * interest in wrapping the provided feature source, then the unchanged provided feature source
     * should be returned. NULL should never be returned.
     *
     * @param featureTypeInfo non NULL GeoServer feature type info
     * @param featureSource non NULL GeoTools feature source
     * @return wrapped feature source or the unchanged provided feature source
     */
    default <T extends FeatureType, U extends Feature> FeatureSource<T, U> wrapFeatureSource(
            FeatureTypeInfo featureTypeInfo, FeatureSource<T, U> featureSource) {
        return featureSource;
    }
}
