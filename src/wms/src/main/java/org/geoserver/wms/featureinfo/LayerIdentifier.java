/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.featureinfo;

import java.util.List;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.feature.FeatureCollection;

/**
 * Extension point that helps run GetFeatureInfo on a specific layer
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface LayerIdentifier {

    /**
     * Returns true if the identifier can handle this layer, false otherwise
     *
     * @param layer
     */
    boolean canHandle(MapLayerInfo layer);

    /**
     * Returns a feature collection identifying the "features" found at the specified location
     *
     * @param params The request parameters
     * @param maxFeatures Max number of features to be returned for this identify
     * @return A list of FeatureCollection objects, each feature in them represent an item the user
     *     clicked on
     */
    List<FeatureCollection> identify(FeatureInfoRequestParameters params, int maxFeatures)
            throws Exception;
}
