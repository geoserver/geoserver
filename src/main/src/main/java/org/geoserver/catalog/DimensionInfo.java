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

    /** Default value for elevation dimension 'units'. * */
    public static final String ELEVATION_UNITS = "EPSG:5030";
    /** Default value for elevation dimension 'unitSymbol'. * */
    public static final String ELEVATION_UNIT_SYMBOL = "m";
    /** Default value for time dimension 'unitSymbol'. * */
    public static final String TIME_UNITS = "ISO8601";

    /** The maximum number of dimension values GeoServer accepts if not otherwise configured */
    public static int DEFAULT_MAX_REQUESTED_DIMENSION_VALUES = 100;

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
}
