/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldType;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** Maps raster attribute table fields, which GDAL stores as text, to Java types. */
public class RasterAttributeTableTypes {

    private static final Logger LOGGER = Logging.getLogger(RasterAttributeTableTypes.class);

    private RasterAttributeTableTypes() {}

    /** Returns the Java type of the given field type, {@link String} for any other type. */
    public static Class<?> getBinding(FieldType type) {
        if (type == null) return String.class;
        switch (type) {
            case Integer:
                return Long.class;
            case Real:
                return Double.class;
            case Boolean:
                return Boolean.class;
            case DateTime:
                return Date.class;
            default:
                // a geometry stays text: a geometry attribute would become the default
                // geometry of the feature, replacing the null one clients get today
                return String.class;
        }
    }

    /** Converts a cell to the type {@link #getBinding(FieldType)} returns, null if it is empty or unreadable. */
    public static Object toValue(FieldType type, String value) {
        // an unset cell is empty, and only a text attribute can carry it as is
        if (value == null || value.isEmpty()) return getBinding(type) == String.class ? value : null;
        if (type == null) return value;
        try {
            switch (type) {
                case Integer:
                    return Long.valueOf(value.trim());
                case Real:
                    return Double.valueOf(value.trim());
                case Boolean:
                    return toBoolean(value.trim());
                case DateTime:
                    // do not read this with Converters: it takes a quarter hour offset for a whole
                    // hour, and GDAL writes offsets of 0, 15, 30 and 45 minutes, so the instant
                    // comes out wrong
                    return Date.from(OffsetDateTime.parse(value.trim()).toInstant());
                default:
                    return value;
            }
        } catch (IllegalArgumentException | DateTimeException e) {
            LOGGER.log(Level.FINE, "Cannot read raster attribute table value " + value + " as " + type, e);
            return null;
        }
    }

    /** Reads the two values GDAL writes for a boolean, null for anything else. */
    private static Boolean toBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
        LOGGER.fine("Cannot read raster attribute table value " + value + " as a boolean");
        return null;
    }
}
