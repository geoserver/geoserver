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

    public static void setupEnvironmentVariables(WMSMapContent mapContent) {
        // setup some SLD variable substitution environment used by rendering transformations
        EnvFunction.setLocalValue("wms_bbox", mapContent.getRenderingArea());
        EnvFunction.setLocalValue(
                "wms_crs", mapContent.getRenderingArea().getCoordinateReferenceSystem());
        EnvFunction.setLocalValue("wms_srs", mapContent.getRequest().getSRS());
        EnvFunction.setLocalValue("wms_width", mapContent.getMapWidth());
        EnvFunction.setLocalValue("wms_height", mapContent.getMapHeight());
        try {
            double scaleDenominator = mapContent.getScaleDenominator(true);
            EnvFunction.setLocalValue("wms_scale_denominator", scaleDenominator);
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Failed to compute the scale denominator, wms_scale_denominator env variable is unset",
                    e);
        }
    }
}
