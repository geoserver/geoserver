/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.utils;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.TypeInfoCollectionWrapper;
import org.geoserver.wms.featureinfo.FeatureCollectionDecorator;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.Name;

/** Class providing methods to retrieve a FeatureTypeInfo. */
public class FeatureTypeInfoUtils {

    /**
     * Retrieve a FeatureTypeInfo.
     *
     * @param catalog a catalog instance.
     * @param collection the FeatureCollection for which retrieve the FeatureTypeInfo.
     * @return the FeatureTypeInfo.
     */
    public static FeatureTypeInfo getFeatureTypeInfo(
            Catalog catalog, FeatureCollection collection) {
        if (collection instanceof TypeInfoCollectionWrapper)
            return ((TypeInfoCollectionWrapper) collection).getFeatureTypeInfo();
        else if (collection instanceof FeatureCollectionDecorator)
            return getFeatureTypeInfo(catalog, ((FeatureCollectionDecorator) collection).getName());
        else return getFeatureTypeInfo(catalog, collection.getSchema().getName());
    }

    /**
     * Retrieve a FeatureTypeInfo.
     *
     * @param catalog a catalog instance.
     * @param collectionId the collectionId, aka the FeatureType name.
     * @return the FeatureTypeInfo.
     */
    public static FeatureTypeInfo getFeatureTypeInfo(Catalog catalog, String collectionId) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(collectionId);
        if (featureType == null) {
            throw new ServiceException(
                    "Unknown collection " + collectionId,
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "collectionId");
        }
        return featureType;
    }

    /**
     * Retrieve a FeatureTypeInfo.
     *
     * @param catalog a catalog instance.
     * @param name the name of the FeatureTypeInfo.
     * @return the FeatureTypeInfo.
     */
    public static FeatureTypeInfo getFeatureTypeInfo(Catalog catalog, Name name) {
        return catalog.getFeatureTypeByName(name);
    }
}
