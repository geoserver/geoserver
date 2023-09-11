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
import org.geotools.api.geometry.BoundingBox;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;

public class GetMapHandler extends RequestObjectHandler {

    static Logger LOGGER = Logging.getLogger("org.geoserver.monitor");

    public GetMapHandler(MonitorConfig config) {
        super("org.geoserver.wms.GetMapRequest", config);
    }

    @Override
    public List<String> getLayers(Object request) {
        @SuppressWarnings("unchecked")
        List<Object> mapLayers = (List<Object>) OwsUtils.get(request, "layers");
        if (mapLayers == null) {
            return null;
        }

        List<String> layers = new ArrayList<>();
        for (Object mapLayer : mapLayers) {
            layers.add((String) OwsUtils.get(mapLayer, "name"));
        }

        return layers;
    }

    @Override
    protected BoundingBox getBBox(Object request) {
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) OwsUtils.get(request, "crs");
        Envelope env = (Envelope) OwsUtils.get(request, "bbox");
        if (env == null) {
            return null;
        }
        BoundingBox bbox = new ReferencedEnvelope(env, crs);

        try {
            return bbox.toBounds(monitorConfig.getBboxCrs());
        } catch (TransformException e) {
            LOGGER.log(Level.WARNING, "Could not transform bounding box to logging CRS", e);
            return null;
        }
    }
}
