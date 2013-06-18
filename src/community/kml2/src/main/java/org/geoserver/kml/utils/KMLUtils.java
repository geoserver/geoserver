/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.utils;

import java.util.List;
import java.util.logging.Logger;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geotools.map.Layer;
import org.geotools.styling.Style;
import org.geotools.util.Converters;
import org.opengis.filter.Filter;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Some convenience methods used by the kml encoders.
 * 
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class KMLUtils {
    /**
     * logger
     */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.kml");

    /**
     * Tolerance used to compare doubles for equality
     */
    static final double TOLERANCE = 1e-6;

    public final static Envelope WORLD_BOUNDS_WGS84 = new Envelope(-180, 180, -90, 90);

    /**
     * Returns the the kmattr value (either specified in the request, or the default one)
     * 
     * @param mapContent
     * @return
     */
    public static boolean getKMAttr(GetMapRequest request, WMS wms) {
        Object kmattr = request.getFormatOptions().get("kmattr");
        if (kmattr == null) {
            kmattr = request.getRawKvp().get("kmattr");
        }
        if (kmattr != null) {
            return Converters.convert(kmattr, Boolean.class);
        } else {
            return wms.getKmlKmAttr();
        }
    }

    /**
     * Returns the the kmplacemark value (either specified in the request, or the default one)
     * 
     * @param mapContent
     * @return
     */
    public static boolean getKmplacemark(GetMapRequest request, WMS wms) {
        Object kmplacemark = request.getFormatOptions().get("kmplacemark");
        if (kmplacemark != null) {
            return Converters.convert(kmplacemark, Boolean.class);
        } else {
            return wms.getKmlPlacemark();
        }
    }

    /**
     * Returns the the kmscore value (either specified in the request, or the default one)
     * 
     * @param mapContent
     * @return
     */
    static int getKmScore(GetMapRequest request, WMS wms) {
        Object kmscore = request.getFormatOptions().get("kmscore");
        if (kmscore != null) {
            return Converters.convert(kmscore, Integer.class);
        } else {
            return wms.getKmScore();
        }
    }

    /**
     * Returns true if the request is GWC compatible
     * 
     * @param mapContent
     * @return
     */
    public static boolean isRequestGWCCompatible(GetMapRequest request, int layerIndex, WMS wms) {
        // check the kml params are the same as the defaults (GWC uses always the defaults)
        boolean requestKmAttr = KMLUtils.getKMAttr(request, wms);
        if (requestKmAttr != wms.getKmlKmAttr()) {
            return false;
        }

        boolean requestKmplacemark = KMLUtils.getKmplacemark(request, wms);
        if (requestKmplacemark != wms.getKmlPlacemark()) {
            return false;
        }

        int requestKmscore = KMLUtils.getKmScore(request, wms);
        if (requestKmscore != wms.getKmScore()) {
            return false;
        }

        // check the layer is local
        if (request.getLayers().get(layerIndex).getType() == MapLayerInfo.TYPE_REMOTE_VECTOR) {
            return false;
        }

        // check the layer is using the default style
        Style requestedStyle = request.getStyles().get(layerIndex);
        Style defaultStyle = request.getLayers().get(layerIndex).getDefaultStyle();
        if (!defaultStyle.equals(requestedStyle)) {
            return false;
        }

        // check there is no extra filtering applied to the layer
        List<Filter> filters = request.getFilter();
        if (filters != null && filters.size() > 0 && filters.get(layerIndex) != Filter.INCLUDE) {
            return false;
        }

        // no fiddling with antialiasing settings
        String antialias = (String) request.getFormatOptions().get("antialias");
        if (antialias != null && !"FULL".equalsIgnoreCase(antialias)) {
            return false;
        }

        // no custom palette
        if (request.getPalette() != null) {
            return false;
        }

        // no custom start index
        if (request.getStartIndex() != null && request.getStartIndex() != 0) {
            return false;
        }

        // no custom max features
        if (request.getMaxFeatures() != null) {
            return false;
        }

        // no sql view params
        if (request.getViewParams() != null && request.getViewParams().size() > 0) {
            return false;
        }

        // ok, it seems everything is the same as GWC cached it
        return true;
    }

    /**
     * Returns true if the request is GWC compatible
     * 
     * @param mapContent
     * @return
     */
    public static boolean isRequestGWCCompatible(WMSMapContent mapContent, Layer layer, WMS wms) {
        int index = getLayerIndex(mapContent, layer);
        return isRequestGWCCompatible(mapContent.getRequest(), index, wms);
    }

    /**
     * Returns the position of the layer in the map context
     */
    public static int getLayerIndex(WMSMapContent mapContent, Layer layer) {
        List<Layer> layers = mapContent.layers();
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i) == layer) {
                return i;
            }
        }

        throw new ServiceException("Unexpected, could not find layer " + layer
                + " in the map context");
    }

}
