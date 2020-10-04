/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.sldservice.rest;

import java.util.Arrays;
import java.util.List;

/**
 * This class represents sldService capabilities. At the moment holds only raster and vector
 * supported classification's methods.
 */
class SldServiceCapabilities {

    private final List<String> vectorClassifications =
            Arrays.asList(
                    "quantile",
                    "jenks",
                    "equalArea",
                    "equalInterval",
                    "uniqueInterval",
                    "standardDeviation");

    private final List<String> rasterClassifications =
            Arrays.asList("quantile", "jenks", "equalArea", "equalInterval", "uniqueInterval");

    List<String> getVectorClassifications() {
        return vectorClassifications;
    }

    List<String> getRasterClassifications() {
        return rasterClassifications;
    }
}
