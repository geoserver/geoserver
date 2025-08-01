/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.geotools.util.factory.Hints.VIRTUAL_TABLE_PARAMETERS;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.filter.function.EnvFunction;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.util.logging.Logging;

/**
 * Helper that injects enviroment variables in the {@link EnvFunction} given a map context
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RenderingVariablesCallback implements GetMapCallback {

    static final Logger LOGGER = Logging.getLogger(RenderingVariablesCallback.class);
    /** The WMS GetMap BBOX, as a {@link org.geotools.geometry.jts.ReferencedEnvelope} */
    public static final String WMS_BBOX = "wms_bbox";
    /** The GetMap SRS, as a {@link org.geotools.api.referencing.crs.CoordinateReferenceSystem} object */
    public static final String WMS_SRS = "wms_srs";
    /** The GetMap width, as an {@link Integer} */
    public static final String WMS_WIDTH = "wms_width";
    /** The GetMap height, as an {@link Integer} */
    public static final String WMS_HEIGHT = "wms_height";
    /** The GetMap scale denominator, as an {@link Double} */
    public static final String WMS_SCALE_DENOMINATOR = "wms_scale_denominator";

    public static final String WMS_CRS = "wms_crs";

    public static void setupEnvironmentVariables(WMSMapContent mapContent) {
        // setup some SLD variable substitution environment used by rendering transformations
        EnvFunction.setLocalValue(WMS_BBOX, mapContent.getRenderingArea());
        EnvFunction.setLocalValue(WMS_CRS, mapContent.getRenderingArea().getCoordinateReferenceSystem());
        EnvFunction.setLocalValue(WMS_SRS, mapContent.getRequest().getSRS());
        EnvFunction.setLocalValue(WMS_WIDTH, mapContent.getMapWidth());
        EnvFunction.setLocalValue(WMS_HEIGHT, mapContent.getMapHeight());
        try {
            double scaleDenominator = mapContent.getScaleDenominator(true);
            EnvFunction.setLocalValue(WMS_SCALE_DENOMINATOR, scaleDenominator);
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Failed to compute the scale denominator, wms_scale_denominator env variable is unset",
                    e);
        }
    }

    @Override
    public GetMapRequest initRequest(GetMapRequest request) {
        return request;
    }

    @Override
    public void initMapContent(WMSMapContent mapContent) {}

    @Override
    public Layer beforeLayer(WMSMapContent mapContent, Layer layer) {
        if (layer instanceof FeatureLayer) {
            @SuppressWarnings("unchecked")
            Map<String, Object> virtualTableParamsMap =
                    (Map<String, Object>) layer.getQuery().getHints().get(VIRTUAL_TABLE_PARAMETERS);
            if (virtualTableParamsMap == null) {
                virtualTableParamsMap = new HashMap<>();
                layer.getQuery().getHints().put(VIRTUAL_TABLE_PARAMETERS, virtualTableParamsMap);
            }

            Map<String, Object> localValues = EnvFunction.getLocalValues();
            setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_BBOX);
            setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_CRS);
            setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_SRS);
            setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_WIDTH);
            setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_HEIGHT);
            setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_SCALE_DENOMINATOR);
        }

        return layer;
    }

    private static void setValueIfNotPresent(
            Map<String, Object> virtualTableParamsMap, Map<String, Object> localValues, String key) {
        if (!virtualTableParamsMap.containsKey(key) && localValues.containsKey(key.toUpperCase())) {
            virtualTableParamsMap.put(key, localValues.get(key.toUpperCase()));
        }
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
}
