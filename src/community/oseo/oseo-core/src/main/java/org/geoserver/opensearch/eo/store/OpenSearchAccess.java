/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * Holds OpenSearchAccess specific constants
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface OpenSearchAccess extends DataAccess<FeatureType, Feature> {

    
}
