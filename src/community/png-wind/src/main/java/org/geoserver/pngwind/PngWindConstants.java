/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind;

/** Constants for PNG-WIND format, including MIME type, output formats, and metadata keys. */
public class PngWindConstants {

    public static final String MIME_TYPE = "image/vnd.png-wind";
    public static final String[] OUTPUT_FORMATS = {MIME_TYPE};
    public static final String U = "U";
    public static final String V = "V";
    public static final String METADATA_CTX_KEY = PngWindConstants.class.getName() + ".REQUEST_CONTEXT";
    public static final String VERSION = "1.0";
    public static final String FORMAT = "PNG-WIND";

    /** Enum representing the types of bands for WIND datasets */
    public enum BandType {
        U,
        V,
        SPEED,
        DIRECTION,
        UNKNOWN
    }
}
