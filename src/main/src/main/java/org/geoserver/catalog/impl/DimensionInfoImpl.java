/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.math.BigDecimal;
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

    /** The default constructor */
    public DimensionInfoImpl() {
        super();
    }

    /**
     * Creates a shallow copy of the given Dimension object
     *
     * @param info
     */
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DimensionInfoImpl [attribute=").append(attribute);
        sb.append(", endAttribute=").append(endAttribute);
        sb.append(", enabled=").append(enabled);
        sb.append(", units=").append(units);
        sb.append(", unitSymbol=").append(unitSymbol);
        sb.append(", presentation=").append(presentation);
        sb.append(", resolution=").append(resolution).append("]");
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
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DimensionInfoImpl other = (DimensionInfoImpl) obj;
        if (attribute == null) {
            if (other.attribute != null) return false;
        } else if (!attribute.equals(other.attribute)) return false;
        if (units == null) {
            if (other.units != null) return false;
        } else if (!units.equals(other.units)) return false;
        if (unitSymbol == null) {
            if (other.unitSymbol != null) return false;
        } else if (!unitSymbol.equals(other.unitSymbol)) return false;
        if (enabled != other.enabled) return false;
        if (presentation == null) {
            if (other.presentation != null) return false;
        } else if (!presentation.equals(other.presentation)) return false;
        if (resolution == null) {
            if (other.resolution != null) return false;
        } else if (!resolution.equals(other.resolution)) return false;
        if (endAttribute == null) {
            if (other.endAttribute != null) return false;
        } else if (!endAttribute.equals(other.endAttribute)) return false;
        return true;
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
