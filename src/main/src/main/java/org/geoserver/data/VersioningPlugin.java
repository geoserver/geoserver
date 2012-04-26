/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.data;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A descriptor for plugins that provide versioning capabilities.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class VersioningPlugin {

    /**
     * Wraps a feature source in a versioning decorator.
     * 
     * @param featureSource The original unversioned feature source.
     * @param featureType The feature type of the source, possibly transformed or modified depending
     *   on the query taking place.
     * @param info The feature type metadata.
     * @param crs The coordinate reference system the data is being requested in. 
     *
     */
    public abstract SimpleFeatureSource wrap(SimpleFeatureSource featureSource, 
        SimpleFeatureType featureType, FeatureTypeInfo info, CoordinateReferenceSystem crs);
}
