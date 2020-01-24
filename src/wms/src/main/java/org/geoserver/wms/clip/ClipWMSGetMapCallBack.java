/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.clip;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapCallback;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/** @author ImranR */
public class ClipWMSGetMapCallBack implements GetMapCallback {

    private static final Logger LOGGER = Logging.getLogger(ClipWMSGetMapCallBack.class.getName());

    private static final WKTReader2 reader = new WKTReader2();

    private static final Pattern SRID_REGEX = Pattern.compile("SRID=[0-9].*");

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
            if (layer instanceof FeatureLayer) {

                // wrap around
                FeatureLayer fl = (FeatureLayer) layer;

                ClippedFeatureSource clippedFS =
                        new ClippedFeatureSource(layer.getFeatureSource(), wktGeom);
                FeatureLayer clippedLayer =
                        new FeatureLayer(clippedFS, fl.getStyle(), fl.getTitle());
                BeanUtilsBean2.getInstance().copyProperties(clippedLayer, fl);
                fl.getUserData().putAll(layer.getUserData());
                return clippedLayer;

            } else if (layer instanceof GridReaderLayer) {

                GridReaderLayer gr = (GridReaderLayer) layer;
                // wrap
                CroppedGridCoverage2DReader croppedGridReader =
                        new CroppedGridCoverage2DReader(gr.getReader(), wktGeom);
                GridReaderLayer croppedGridLayer =
                        new GridReaderLayer(croppedGridReader, layer.getStyle());
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

    public static synchronized Geometry readGeometry(
            final String wkt, final CoordinateReferenceSystem mapCRS) throws Exception {
        String[] wktContents = wkt.split(";");
        Geometry geom = reader.read(wktContents[wktContents.length - 1]);
        if (!(geom.getClass().isAssignableFrom(Polygon.class)
                || geom.getClass().isAssignableFrom(MultiPolygon.class)))
            throw new ServiceException(
                    "Clip must be a polygon or multipolygon", "InvalidParameterValue", "clip");
        // parse SRID if passed
        // looking for a pattern srid=4326:Polygon(...)
        if (wktContents.length == 2 && SRID_REGEX.matcher(wktContents[0].toUpperCase()).matches()) {
            String sridString = wktContents[0].split("=")[1];
            // force xy
            CoordinateReferenceSystem geomCRS = CRS.decode("EPSG:" + sridString, true);
            CoordinateReferenceSystem mapCRSXY =
                    CRS.decode("EPSG:" + CRS.lookupEpsgCode(mapCRS, false), true);
            if (CRS.isTransformationRequired(mapCRSXY, geomCRS)) {
                MathTransform transform = CRS.findMathTransform(geomCRS, mapCRSXY);
                geom = JTS.transform(geom, transform);
            }
        }
        // finally assign map crs
        geom.setSRID(CRS.lookupEpsgCode(mapCRS, false));
        return geom;
    }
}
