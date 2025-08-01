/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.geotools.util.factory.Hints.VIRTUAL_TABLE_PARAMETERS;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.api.data.Query;
import org.geotools.filter.function.EnvFunction;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.util.logging.Logging;

/**
 * Helper that injects enviroment variables in the {@link EnvFunction} given a map context
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RenderingVariables {

    static final Logger LOGGER = Logging.getLogger(RenderingVariables.class);
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

    /**
     * Set the WMS specific request parameters to EnvFunction.
     *
     * @param params
     */
    public static void setupEnvironmentVariables(FeatureInfoRequestParameters params) {
        EnvFunction.setLocalValue(WMS_BBOX, params.getRequestedBounds());
        EnvFunction.setLocalValue(WMS_SRS, params.getGetMapRequest().getSRS());
        EnvFunction.setLocalValue(WMS_WIDTH, params.getWidth());
        EnvFunction.setLocalValue(WMS_HEIGHT, params.getHeight());
        EnvFunction.setLocalValue(WMS_SCALE_DENOMINATOR, params.getScaleDenominator());
    }

    public static void setupEnvironmentVariables(WMSMapContent mapContent) {
        EnvFunction.setLocalValue(WMS_BBOX, mapContent.getRenderingArea());
        EnvFunction.setLocalValue("wms_crs", mapContent.getRenderingArea().getCoordinateReferenceSystem());
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

    /**
     * Update the layer to have query with query hints from env.
     *
     * @param layer
     */
    public static void setQueryHintsFromEnv(FeatureLayer layer) {
        Query query = setHintsToQuery(layer.getQuery());
        layer.setQuery(query);
    }

    /**
     * Propagate WMS specific parameters from env variables to query hints.
     *
     * @param query
     * @return
     */
    public static Query setHintsToQuery(Query query) {
        Query resultQuery = new Query(query);
        @SuppressWarnings("unchecked")
        Map<String, String> virtualTableParamsMap =
                (Map<String, String>) resultQuery.getHints().get(VIRTUAL_TABLE_PARAMETERS);
        if (virtualTableParamsMap == null) {
            virtualTableParamsMap = new HashMap<>();
            resultQuery.getHints().put(VIRTUAL_TABLE_PARAMETERS, virtualTableParamsMap);
        }

        Map<String, Object> localValues = EnvFunction.getLocalValues();
        setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_BBOX, (input) -> {
            ReferencedEnvelope envelope = (ReferencedEnvelope) input;
            double minX = envelope.getMinX();
            double minY = envelope.getMinY();
            double maxX = envelope.getMaxX();
            double maxY = envelope.getMaxY();

            return String.format("%.6f,%.6f,%.6f,%.6f", minX, minY, maxX, maxY);
        });
        setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_SRS);
        setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_WIDTH, String::valueOf);
        setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_HEIGHT, String::valueOf);
        setValueIfNotPresent(virtualTableParamsMap, localValues, WMS_SCALE_DENOMINATOR, String::valueOf);
        return resultQuery;
    }

    private static void setValueIfNotPresent(
            Map<String, String> virtualTableParamsMap, Map<String, Object> localValues, String key) {
        // We need to uppercase the key for localValues because keys are uppercased when added to EnvFunction
        String uppercasedKey = key.toUpperCase();
        if (!virtualTableParamsMap.containsKey(key) && localValues.containsKey(uppercasedKey)) {
            virtualTableParamsMap.put(key, (String) localValues.get(uppercasedKey));
        }
    }

    private static void setValueIfNotPresent(
            Map<String, String> virtualTableParamsMap,
            Map<String, Object> localValues,
            String key,
            Function<Object, String> transformer) {
        // We need to uppercase the key for localValues because keys are uppercased when added to EnvFunction
        String uppercasedKey = key.toUpperCase();
        if (!virtualTableParamsMap.containsKey(key) && localValues.containsKey(uppercasedKey)) {
            virtualTableParamsMap.put(key, transformer.apply(localValues.get(uppercasedKey)));
        }
    }
}
