/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import java.util.ArrayList;
import java.util.List;

/**
 * Enum of WMS-EO layer types.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
public enum EoLayerType {
    BROWSE_IMAGE,
    COVERAGE_OUTLINE,
    BAND_COVERAGE,
    GEOPHYSICAL_PARAMETER,
    BITMASK,
    IGNORE;

    /** Key used in LayerInfo metadata to store EO Layer type */
    public static final String KEY = "WMSEO-LAYER";

    /** Returns a list of the "normal" layer types */
    public static List<EoLayerType> getRegularTypes() {
        List<EoLayerType> result = new ArrayList<EoLayerType>();
        result.add(EoLayerType.BROWSE_IMAGE);
        result.add(EoLayerType.BAND_COVERAGE);
        result.add(EoLayerType.COVERAGE_OUTLINE);
        result.add(EoLayerType.GEOPHYSICAL_PARAMETER);
        result.add(EoLayerType.BITMASK);
        return result;
    }

    /** Returns a list of the raster layer types */
    public static List<EoLayerType> getRasterTypes(boolean includeIgnore) {
        List<EoLayerType> result = new ArrayList<EoLayerType>();
        if (includeIgnore) {
            result.add(EoLayerType.IGNORE);
        }
        result.add(EoLayerType.BROWSE_IMAGE);
        result.add(EoLayerType.BAND_COVERAGE);
        result.add(EoLayerType.GEOPHYSICAL_PARAMETER);
        result.add(EoLayerType.BITMASK);
        return result;
    }
}
