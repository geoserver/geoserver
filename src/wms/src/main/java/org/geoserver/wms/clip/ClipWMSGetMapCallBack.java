/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.clip;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.geoserver.ows.kvp.ClipGeometryParser;
import org.geoserver.wms.CachedGridReaderLayer;
import org.geoserver.wms.GetMapCallback;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.JTS;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/** @author ImranR */
public class ClipWMSGetMapCallBack implements GetMapCallback {

    private static final Logger LOGGER = Logging.getLogger(ClipWMSGetMapCallBack.class.getName());

    @Override
    public GetMapRequest initRequest(GetMapRequest request) {
        return request;
    }

    @Override
    public void initMapContent(WMSMapContent mapContent) {}

    @Override
    public Layer beforeLayer(WMSMapContent mapContent, Layer layer) {

        // read geometry from WMS request
        Geometry wktGeom = mapContent.getRequest().getClip();
        if (wktGeom == null) return layer;

        Geometry bboxGeom = JTS.toGeometry(mapContent.getRequest().getBbox());
        // check: if wkt area fully contains bbox
        if (wktGeom.covers(bboxGeom)) return layer;
        try {
            if (layer instanceof FeatureLayer fl) {

                FeatureSource<?, ?> clippedFS = new ClippedFeatureSource<>(layer.getFeatureSource(), wktGeom);
                FeatureLayer clippedLayer = new FeatureLayer(clippedFS, fl.getStyle(), fl.getTitle());
                BeanUtilsBean2.getInstance().copyProperties(clippedLayer, fl);
                fl.getUserData().putAll(layer.getUserData());
                return clippedLayer;

            } else if (layer instanceof GridReaderLayer gr) {
                // wrap
                CroppedGridCoverage2DReader croppedGridReader =
                        new CroppedGridCoverage2DReader(gr.getReader(), wktGeom);
                CachedGridReaderLayer croppedGridLayer =
                        new CachedGridReaderLayer(croppedGridReader, layer.getStyle(), gr.getParams());
                BeanUtilsBean2.getInstance().copyProperties(croppedGridLayer, gr);
                croppedGridLayer.getUserData().putAll(layer.getUserData());
                return croppedGridLayer;
            }
        } catch (Exception e) {
            LOGGER.severe("Error occurred while clipping layer " + layer.getTitle());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return layer;
        }

        return layer;
    }

    @Override
    public WMSMapContent beforeRender(WMSMapContent mapContent) {
        return mapContent;
    }

    @Override
    public WebMap finished(WebMap map) {
        return map;
    }

    @Override
    public void failed(Throwable t) {}

    public static synchronized Geometry readGeometry(final String wkt, final CoordinateReferenceSystem mapCRS)
            throws Exception {
        return ClipGeometryParser.readGeometry(wkt, mapCRS);
    }
}
