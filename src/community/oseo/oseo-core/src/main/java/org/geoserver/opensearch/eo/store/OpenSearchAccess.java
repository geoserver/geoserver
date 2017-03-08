/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Holds OpenSearchAccess specific constants
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface OpenSearchAccess extends DataAccess<FeatureType, Feature> {

    public static String EO_NAMESPACE = "http://a9.com/-/opensearch/extensions/eo/1.0/";
    
    public static String GEO_NAMESPACE = "http://a9.com/-/opensearch/extensions/geo/1.0/";

    /**
     * Returns the name of the feature type backing EO collections

     */
    Name getCollectionName();

    
}
