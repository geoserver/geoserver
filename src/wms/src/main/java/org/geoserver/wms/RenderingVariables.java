/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.filter.function.EnvFunction;
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
    /**
     * The GetMap SRS, as a {@link org.geotools.api.referencing.crs.CoordinateReferenceSystem}
     * object
     */
    private static final String WMS_SRS = "wms_srs";
    /** The GetMap width, as an {@link Integer} */
    private static final String WMS_WIDTH = "wms_width";
    /** The GetMap height, as an {@link Integer} */
    private static final String WMS_HEIGHT = "wms_height";
    /** The GetMap scale denominator, as an {@link Double} */
    private static final String WMS_SCALE_DENOMINATOR = "wms_scale_denominator";

    public static void setupEnvironmentVariables(WMSMapContent mapContent) {
        // setup some SLD variable substitution environment used by rendering transformations
        EnvFunction.setLocalValue(WMS_BBOX, mapContent.getRenderingArea());
        EnvFunction.setLocalValue(
                "wms_crs", mapContent.getRenderingArea().getCoordinateReferenceSystem());
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
}
