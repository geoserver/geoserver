/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Represents a dimension, such as the standard TIME and ELEVATION ones, but could be a custom one
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface DimensionInfo extends Serializable {

    public static final String CUSTOM_DIM_PREFIX = "DIM_";

    /** Default value for elevation dimension 'units'. * */
    public static final String ELEVATION_UNITS = "EPSG:5030";

    /** Default value for elevation dimension 'unitSymbol'. * */
    public static final String ELEVATION_UNIT_SYMBOL = "m";

    /** Default value for time dimension 'unitSymbol'. * */
    public static final String TIME_UNITS = "ISO8601";

    /** The maximum number of dimension values GeoServer accepts if not otherwise configured */
    public static int DEFAULT_MAX_REQUESTED_DIMENSION_VALUES = 100;

    /**
     * Controls how nearest match behaves in combination with acceptable interval. If set to
     * IGNORE_ON_FAIL, the failed match will be ignored and the original dimension value used. If
     * set to THROW_ON_FAIL, the failed match will cause a service exception to be thrown.
     */
    public static enum NearestFailBehavior {
        /** On failed match, ignore the nearest lookup and use the original dimension value */
        IGNORE,
        /** On failed match, throw a service exception */
        EXCEPTION
    }

    /** System property key to control the nearest fail behavior */
    public static String NEAREST_FAIL_BEHAVIOR_KEY = "org.geoserver.wms.nearestFail";

    /** Default value for nearest fail behavior */
    public static final NearestFailBehavior DEFAULT_NEAREST_FAIL =
            NearestFailBehavior.valueOf(
                    System.getProperty(
                            NEAREST_FAIL_BEHAVIOR_KEY, NearestFailBehavior.IGNORE.name()));

    /**
     * Returns the KVP key used for a given dimension name, which can be either a standard one
     * (returned as-is) or a custom one (prefixed with {@link #CUSTOM_DIM_PREFIX}) custom one
     */
    public static String getDimensionKey(String name) {
        if ("time".equalsIgnoreCase(name) || "elevation".equalsIgnoreCase(name)) {
            return name;
        }
        return CUSTOM_DIM_PREFIX + name.toUpperCase();
    }

    /** Whether this dimension is enabled or not */
    public boolean isEnabled();

    /** Sets the dimension as enabled, or not */
    public void setEnabled(boolean enabled);

    /** The attribute on which the dimension is based. Used only for vector data */
    public String getAttribute();

    public void setAttribute(String attribute);

    /**
     * The attribute on which the end of the dimension is based. Used only for vector data. This
     * attribute is optional.
     */
    public String getEndAttribute();

    public void setEndAttribute(String attribute);

    /** The way the dimension is going to be presented in the capabilities documents */
    public DimensionPresentation getPresentation();

    public void setPresentation(DimensionPresentation presentation);

    /**
     * The interval resolution in case {@link DimensionPresentation#DISCRETE_INTERVAL} presentation
     * has been chosen (it can be a representation of a elevation resolution or a time interval in
     * milliseconds)
     */
    public BigDecimal getResolution();

    public void setResolution(BigDecimal resolution);

    /**
     * The units attribute for the elevation dimension. This method has no affect on the time
     * dimension.
     *
     * @return the value for units
     */
    public String getUnits();

    public void setUnits(String units);

    /**
     * The unitSymbol attribute for the elevation dimension. This method has no affect on the time
     * dimension.
     *
     * @return the value for unitSymbol
     */
    public String getUnitSymbol();

    public void setUnitSymbol(String unitSymbol);

    /**
     * The setting for selecting the default value for this dimension.
     *
     * @return the current default value setting
     */
    public DimensionDefaultValueSetting getDefaultValue();

    public void setDefaultValue(DimensionDefaultValueSetting defaultValue);

    /**
     * Returns true if the nearest match behavior is implemented. Right now it's only available for
     * the TIME dimension, support for other dimensions might come later
     */
    public boolean isNearestMatchEnabled();

    /** Enables/disables nearest match. */
    public void setNearestMatchEnabled(boolean nearestMatch);

    /**
     * Returns true if the nearest match behavior is implemented for raw data requests. Right now
     * it's only available for the TIME dimension, support for other dimensions might come later.
     * Raw Nearest Match means nearest match on WCS when dealing with a coverage layer or WFS for
     * feature layer. Right now it's only available for WCS, support for other services might come
     * later.
     */
    public boolean isRawNearestMatchEnabled();

    /** Enables/disables raw nearest match. */
    public void setRawNearestMatchEnabled(boolean rawNearestMatch);

    /**
     * Returns a string specifying the search range. Can be empty, a single value (to be parsed in
     * the data type of the dimension, in particular, it will be a ISO period for times) or a
     * {code}before/after{code} range specifying how far to search from the requested value (e.g.,
     * {code}PT12H/PT1H{code} to allow searching 12 hours in the past but only 1 hour in the
     * future).
     */
    public String getAcceptableInterval();

    /**
     * Allows setting the search range for nearest matches, see also {@link
     * #getAcceptableInterval()}.
     */
    public void setAcceptableInterval(String acceptableInterval);

    /**
     * Returns the start value for the data range, which will be used in capabilities documents. Has
     * to be either numeric, of type ISO8601 DateTime or the string "PRESENT" which will use the
     * current DateTime when the capabilities document is generated.
     *
     * @return the value for startValue
     */
    public String getStartValue();

    /** Sets the startValue for the data range */
    public void setStartValue(String startValue);

    /**
     * Returns the end value for the data range, which will be used in capabilities documents. Has
     * to be either numeric, of type ISO8601 DateTime or the string "PRESENT" which will use the
     * current DateTime when the capabilities document is generated.
     *
     * @return the value for endValue
     */
    public String getEndValue();

    /** Sets the endValue for the data range */
    public void setEndValue(String endValue);

    /**
     * Returns tje current nearest {@link NearestFailBehavior}. If unset, the default will be {@link
     * NearestFailBehavior#IGNORE}
     */
    public NearestFailBehavior getNearestFailBehavior();

    /** Sets the {@link NearestFailBehavior} */
    public void setNearestFailBehavior(NearestFailBehavior matchBehavior);
}
