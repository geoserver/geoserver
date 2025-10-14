/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.featureinfo;

import java.util.List;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;

/**
 * Extension point that helps run GetFeatureInfo on a specific layer
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface LayerIdentifier<T> {

    /** Returns true if the identifier can handle this layer, false otherwise */
    boolean canHandle(MapLayerInfo layer);

    /**
     * Returns a feature collection identifying the "features" found at the specified location
     *
     * @param params The request parameters
     * @param maxFeatures Max number of features to be returned for this identify
     * @return A list of FeatureCollection objects, each feature in them represent an item the user clicked on
     */
    List<FeatureCollection> identify(FeatureInfoRequestParameters params, int maxFeatures) throws Exception;

    /**
     * Returns a clipped feature or grid source
     *
     * @param params The request parameters
     * @param fs source to be clipped if GetFeatureInfo includes WMS Vendor parameter `clip`
     */
    T handleClipParam(FeatureInfoRequestParameters params, T fs);

    /** Utility for implementors, allows to find a search area around the middle of the requested area */
    static ReferencedEnvelope getEnvelopeFilter(FeatureInfoRequestParameters params, double radius) {
        final int x = params.getX();
        final int y = params.getY();
        final ReferencedEnvelope bbox = params.getRequestedBounds();
        final int width = params.getWidth();
        final int height = params.getHeight();
        Coordinate upperLeft = WMS.pixelToWorld(x - radius, y - radius, bbox, width, height);
        Coordinate lowerRight = WMS.pixelToWorld(x + radius, y + radius, bbox, width, height);

        return new ReferencedEnvelope(
                upperLeft.x, lowerRight.x, lowerRight.y, upperLeft.y, bbox.getCoordinateReferenceSystem());
    }
}
