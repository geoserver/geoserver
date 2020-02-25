/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.HashMap;
import java.util.Map;
import org.geotools.util.Version;

/**
 * Provides lookups for OWS error codes based on WMS version.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public enum WMSErrorCode {

    /** Error code for client specifying an invalid srs/crs in a GetMap request. */
    INVALID_CRS("InvalidSRS", "1.1.1", "InvalidSRS", "1.3.0", "InvalidCRS"),

    /** Error code for client specifying a non queryable layer in a GetFeatureInfo request. */
    LAYER_NOT_QUERYABLE(
            "OperationNotSupported",
            "1.1.1",
            "OperationNotSupported",
            "1.3.0",
            "LayerNotQueryable");

    String defaultCode;
    Map<Version, String> codes;

    private WMSErrorCode(String defaultCode, String... mappings) {
        if (mappings.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of version/code mappings");
        }

        codes = new HashMap();
        for (int i = 0; i < mappings.length - 1; i += 2) {
            codes.put(new Version(mappings[i]), mappings[i + 1]);
        }

        this.defaultCode = defaultCode;
    }

    /**
     * Looks up the error code.
     *
     * @param version The wms version.
     * @return The error code, or the default if the version did not match.
     */
    public String get(String version) {
        if (version != null) {
            return get(new Version(version));
        } else {
            return get(new Version("1.1.1"));
        }
    }

    /**
     * Looks up the error code.
     *
     * @param version The wms version.
     * @return The error code, or the default if the version did not match.
     */
    public String get(Version version) {
        String code = codes.get(version);
        return code != null ? code : defaultCode;
    }
}
