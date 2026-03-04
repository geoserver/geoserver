/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind;

import java.util.Locale;

public class PngWindConstants {

    public static final String PROP_DIR_CONV_KEY = "pngwind.dir.convention";
    public static final String PROP_DIR_UNIT_KEY = "pngwind.dir.unit";

    public static final String MIME_TYPE = "image/vnd.png-wind";

    public static final String[] OUTPUT_FORMATS = {MIME_TYPE};
    public static final String U = "U";
    public static final String V = "V";

    public static PngWindTransform.DirConvention  DIR_CONVENTION =  PngWindTransform.DirConvention.FROM;

    public static PngWindTransform.DirUnit DIR_UNIT = PngWindTransform.DirUnit.DEG;;

    public static final String METADATA_CTX_KEY =
            PngWindConstants.class.getName() + ".REQUEST_CONTEXT";

    static {
        String v = System.getProperty(PROP_DIR_CONV_KEY, "FROM").trim().toUpperCase(Locale.ROOT);
        try {
            DIR_CONVENTION =  PngWindTransform.DirConvention.valueOf(v);
        } catch (Exception ignored) {

        }
        v = System.getProperty(PROP_DIR_UNIT_KEY, "DEG").trim().toUpperCase(Locale.ROOT);
        try {
            DIR_UNIT = PngWindTransform.DirUnit.valueOf(v);
        } catch (Exception ignored) {
        }
    }

}
