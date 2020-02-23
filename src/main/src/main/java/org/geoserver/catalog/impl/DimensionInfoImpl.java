/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.math.BigDecimal;
import java.util.Objects;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;

/**
 * Configuration about a dimension, such as time or elevation (theoretically could be a custom one
 * too)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DimensionInfoImpl implements DimensionInfo {

    /** serialVersionUID */
    private static final long serialVersionUID = -2978192474130857785L;

    boolean enabled;

    String attribute;

    String endAttribute;

    DimensionPresentation presentation;

    BigDecimal resolution;

    String units;

    String unitSymbol;

    DimensionDefaultValueSetting defaultValue;

    Boolean nearestMatchEnabled;

    String acceptableInterval;

    /** The default constructor */
    public DimensionInfoImpl() {
        super();
    }

    /** Creates a shallow copy of the given Dimension object */
    public DimensionInfoImpl(DimensionInfo info) {
        super();
        this.enabled = info.isEnabled();
        this.attribute = info.getAttribute();
        this.endAttribute = info.getEndAttribute();
        this.presentation = info.getPresentation();
        this.resolution = info.getResolution();
        this.units = info.getUnits();
        this.unitSymbol = info.getUnitSymbol();
        this.defaultValue = info.getDefaultValue();
        this.enabled = info.isEnabled();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getEndAttribute() {
        return this.endAttribute;
    }

    public void setEndAttribute(String attribute) {
        this.endAttribute = attribute;
    }

    public DimensionPresentation getPresentation() {
        return presentation;
    }

    public void setPresentation(DimensionPresentation presentation) {
        this.presentation = presentation;
    }

    public BigDecimal getResolution() {
        return resolution;
    }

    public void setResolution(BigDecimal resolution) {
        this.resolution = resolution;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getUnitSymbol() {
        return unitSymbol;
    }

    public void setUnitSymbol(String unitSymbol) {
        this.unitSymbol = unitSymbol;
    }

    public boolean isNearestMatchEnabled() {
        // for backwards compatiblity we allow nearest search to be null
        return nearestMatchEnabled == null ? false : nearestMatchEnabled;
    }

    public void setNearestMatchEnabled(boolean nearestMatchEnabled) {
        this.nearestMatchEnabled = nearestMatchEnabled;
    }

    @Override
    public String getAcceptableInterval() {
        return acceptableInterval;
    }

    @Override
    public void setAcceptableInterval(String searchRange) {
        this.acceptableInterval = searchRange;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DimensionInfoImpl [attribute=").append(attribute);
        sb.append(", endAttribute=").append(endAttribute);
        sb.append(", enabled=").append(enabled);
        sb.append(", units=").append(units);
        sb.append(", unitSymbol=").append(unitSymbol);
        sb.append(", presentation=").append(presentation);
        sb.append(", resolution=").append(resolution);
        sb.append(", nearest=").append(nearestMatchEnabled);
        sb.append(", acceptableInterval=").append(acceptableInterval);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
        result = prime * result + ((endAttribute == null) ? 0 : endAttribute.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((units == null) ? 0 : units.hashCode());
        result = prime * result + ((unitSymbol == null) ? 0 : unitSymbol.hashCode());
        result = prime * result + ((presentation == null) ? 0 : presentation.hashCode());
        result = prime * result + ((resolution == null) ? 0 : resolution.hashCode());
        result = prime * result + ((nearestMatchEnabled == null) ? 0 : resolution.hashCode());
        result = prime * result + ((acceptableInterval == null) ? 0 : resolution.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DimensionInfoImpl that = (DimensionInfoImpl) o;
        return enabled == that.enabled
                && Objects.equals(attribute, that.attribute)
                && Objects.equals(endAttribute, that.endAttribute)
                && presentation == that.presentation
                && Objects.equals(resolution, that.resolution)
                && Objects.equals(units, that.units)
                && Objects.equals(unitSymbol, that.unitSymbol)
                && Objects.equals(defaultValue, that.defaultValue)
                && Objects.equals(nearestMatchEnabled, that.nearestMatchEnabled)
                && Objects.equals(acceptableInterval, that.acceptableInterval);
    }

    @Override
    public DimensionDefaultValueSetting getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public void setDefaultValue(DimensionDefaultValueSetting defaultValue) {
        this.defaultValue = defaultValue;
    }
}
