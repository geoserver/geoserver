/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.math.BigDecimal;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;

/**
 * Configuration about a dimension, such as time or elevation (theoretically could be a custom one
 * too)
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class DimensionInfoImpl implements DimensionInfo {

    boolean enabled;

    String attribute;

    DimensionPresentation presentation;

    BigDecimal resolution;

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


    @Override
    public String toString() {
        return "DimensionInfoImpl [attribute=" + attribute + ", enabled=" + enabled
                + ", presentation=" + presentation + ", resolution=" + resolution + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((presentation == null) ? 0 : presentation.hashCode());
        result = prime * result + ((resolution == null) ? 0 : resolution.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DimensionInfoImpl other = (DimensionInfoImpl) obj;
        if (attribute == null) {
            if (other.attribute != null)
                return false;
        } else if (!attribute.equals(other.attribute))
            return false;
        if (enabled != other.enabled)
            return false;
        if (presentation == null) {
            if (other.presentation != null)
                return false;
        } else if (!presentation.equals(other.presentation))
            return false;
        if (resolution == null) {
            if (other.resolution != null)
                return false;
        } else if (!resolution.equals(other.resolution))
            return false;
        return true;
    }

}
