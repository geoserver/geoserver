/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import java.io.IOException;
import java.util.Map;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;

import com.vividsolutions.jts.geom.Geometry;

public interface VectorTileBuilder {

    void addFeature(String layerName, String featureId, String geometryName, Geometry geometry,
            Map<String, Object> properties);

    WebMap build(WMSMapContent mapContent) throws IOException;

}
