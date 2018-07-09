/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.ows.wms;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.ows.RequestObjectHandler;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

public class GetFeatureInfoHandler extends RequestObjectHandler {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

    public GetFeatureInfoHandler(MonitorConfig config) {
        super("org.geoserver.wms.GetFeatureInfoRequest", config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getLayers(Object request) {
        List<Object> queryLayers = (List<Object>) OwsUtils.get(request, "queryLayers");
        if (queryLayers == null) {
            return null;
        }

        List<String> layers = new ArrayList<String>();
        for (int i = 0; i < queryLayers.size(); i++) {
            layers.add((String) OwsUtils.get(queryLayers.get(i), "name"));
        }

        return layers;
    }

    @Override
    protected BoundingBox getBBox(Object request) {
        Object gmr = OwsUtils.get(request, "getMapRequest");
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) OwsUtils.get(gmr, "crs");
        Envelope mapEnv = (Envelope) OwsUtils.get(gmr, "bbox");
        ReferencedEnvelope mapBbox = new ReferencedEnvelope(mapEnv, crs);
        int x = (Integer) OwsUtils.get(request, "xPixel");
        int y = (Integer) OwsUtils.get(request, "yPixel");
        int width = (Integer) OwsUtils.get(gmr, "width");
        int height = (Integer) OwsUtils.get(gmr, "height");

        Coordinate coord = org.geoserver.wms.WMS.pixelToWorld(x, y, mapBbox, width, height);

        try {
            return new ReferencedEnvelope(new Envelope(coord), crs)
                    .toBounds(monitorConfig.getBboxCrs());
        } catch (TransformException e) {
            LOGGER.log(Level.WARNING, "Could not transform bounding box to logging CRS", e);
            return null;
        }
    }
}
