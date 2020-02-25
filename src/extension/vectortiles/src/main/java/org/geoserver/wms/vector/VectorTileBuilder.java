/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import java.io.IOException;
import java.util.Map;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.locationtech.jts.geom.Geometry;

/** Collects features into a vector tile */
public interface VectorTileBuilder {

    /**
     * Add a feature to the tile
     *
     * @param layerName The name of the feature set
     * @param featureId The identifier of the feature within the feature set
     * @param geometryName The name of the geometry property
     * @param geometry The geometry value
     * @param properties The non-geometry attributes of the feature
     */
    void addFeature(
            String layerName,
            String featureId,
            String geometryName,
            Geometry geometry,
            Map<String, Object> properties);

    /**
     * Build the tile
     *
     * @param mapContent The context for building the tile.
     * @return A WebMap containing the completed tile
     */
    WebMap build(WMSMapContent mapContent) throws IOException;
}
