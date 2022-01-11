/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Extension point to initialize/cleanup the underlying resource of a feature type with custom
 * informations taken from its metadata <br>
 * This may be useful when the resource configuration is dynamic and based on informations provided
 * by user as in case of {@link org.geotools.jdbc.VirtualTable}
 *
 * <p>The extension point is used as follows:
 *
 * <pre>
 * featureTypeInitializers = GeoServerExtensions.extensions(FeatureTypeInitializer.class);
 * for(FeatureTypeInitializer fti : featureTypeInitializers){
 *      if(fti.canHandle(info,dataAccess)){
 *              fti.initialize(info,dataAccess);
 *      }
 * }
 * </pre>
 *
 * @see {@link FeatureTypeInfo#getMetadata()}
 * @see {@link ResourcePool#getCacheableFeatureType}
 * @see {@link ResourcePool#getNonCacheableFeatureType}
 */
public interface FeatureTypeCallback {

    /** Checks if this initializer can handle the specified resource handle */
    boolean canHandle(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess);

    /**
     * Initializes the specified feature type in the specified data access. If temporaryName is
     * provided, it means the initializer should try to initializer the feature type with the given
     * temporary name, unless the feature type already exists.
     *
     * @return true if the initialization used the temporary name, false otherwise
     */
    boolean initialize(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            Name temporaryName)
            throws IOException;

    /** Prepares for the feature type to be flushed */
    void flush(
            FeatureTypeInfo info, DataAccess<? extends FeatureType, ? extends Feature> dataAccess)
            throws IOException;

    /**
     * Performs any cleanup necessary to clean up the layer from the specified store. In case a
     * previous initialization used a temporary name, it will be passed down and the initiliazer
     * should use it for cleanup purposes
     */
    void dispose(
            FeatureTypeInfo info,
            DataAccess<? extends FeatureType, ? extends Feature> dataAccess,
            Name temporaryName)
            throws IOException;
}
