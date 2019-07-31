/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import static org.geotools.util.Utilities.ensureArgumentNonNull;

import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.Objects;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.impl.DimensionInfoImpl;

/**
 * DimensionInfo implementation with extra attributes for use in {@link
 * VectorCustomDimensionEditor}.
 *
 * @author Fernando Mino - Geosolutions
 */
public class VectorCustomDimensionEntry implements DimensionInfo {

    private String key;
    private String formerKey;
    private DimensionInfo dimensionInfo;

    public VectorCustomDimensionEntry() {
        this.dimensionInfo = new DimensionInfoImpl();
    }

    public VectorCustomDimensionEntry(Entry<String, DimensionInfo> entry) {
        ensureArgumentNonNull("entry", entry);
        ensureArgumentNonNull("entry.key", entry.getKey());
        ensureArgumentNonNull("entry.value", entry.getValue());
        this.key = entry.getKey();
        this.formerKey = entry.getKey();
        this.dimensionInfo = entry.getValue();
    }

    public VectorCustomDimensionEntry(VectorCustomDimensionEntry entry) {
        ensureArgumentNonNull("entry", entry);
        ensureArgumentNonNull("entry.dimensionInfo", entry.getDimensionInfo());
        this.dimensionInfo = new DimensionInfoImpl(entry.getDimensionInfo());
        this.key = entry.getKey();
        this.formerKey = entry.getFormerKey();
    }

    public boolean isEnabled() {
        return dimensionInfo.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        dimensionInfo.setEnabled(enabled);
    }

    public String getAttribute() {
        return dimensionInfo.getAttribute();
    }

    public void setAttribute(String attribute) {
        dimensionInfo.setAttribute(attribute);
    }

    public String getEndAttribute() {
        return dimensionInfo.getEndAttribute();
    }

    public void setEndAttribute(String attribute) {
        dimensionInfo.setEndAttribute(attribute);
    }

    public DimensionPresentation getPresentation() {
        return dimensionInfo.getPresentation();
    }

    public void setPresentation(DimensionPresentation presentation) {
        dimensionInfo.setPresentation(presentation);
    }

    public BigDecimal getResolution() {
        return dimensionInfo.getResolution();
    }

    public void setResolution(BigDecimal resolution) {
        dimensionInfo.setResolution(resolution);
    }

    public String getUnits() {
        return dimensionInfo.getUnits();
    }

    public void setUnits(String units) {
        dimensionInfo.setUnits(units);
    }

    public String getUnitSymbol() {
        return dimensionInfo.getUnitSymbol();
    }

    public void setUnitSymbol(String unitSymbol) {
        dimensionInfo.setUnitSymbol(unitSymbol);
    }

    public DimensionDefaultValueSetting getDefaultValue() {
        return dimensionInfo.getDefaultValue();
    }

    public void setDefaultValue(DimensionDefaultValueSetting defaultValue) {
        dimensionInfo.setDefaultValue(defaultValue);
    }

    public boolean isNearestMatchEnabled() {
        return dimensionInfo.isNearestMatchEnabled();
    }

    public void setNearestMatchEnabled(boolean nearestMatch) {
        dimensionInfo.setNearestMatchEnabled(nearestMatch);
    }

    public String getAcceptableInterval() {
        return dimensionInfo.getAcceptableInterval();
    }

    public void setAcceptableInterval(String acceptableInterval) {
        dimensionInfo.setAcceptableInterval(acceptableInterval);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFormerKey() {
        return formerKey;
    }

    public void setFormerKey(String formerKey) {
        this.formerKey = formerKey;
    }

    public DimensionInfo getDimensionInfo() {
        return dimensionInfo;
    }

    public void setDimensionInfo(DimensionInfo dimensionInfo) {
        this.dimensionInfo = dimensionInfo;
    }

    public String getKeyNoPrefixed() {
        return this.key.replaceFirst("dim_", "");
    }

    public void setKeyNoPrefixed(String key) {
        this.key = "dim_" + key;
    }

    public boolean hasModifiedKey() {
        return !Objects.equals(this.key, this.formerKey);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dimensionInfo == null) ? 0 : dimensionInfo.hashCode());
        result = prime * result + ((formerKey == null) ? 0 : formerKey.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        VectorCustomDimensionEntry other = (VectorCustomDimensionEntry) obj;
        if (dimensionInfo == null) {
            if (other.dimensionInfo != null) return false;
        } else if (!dimensionInfo.equals(other.dimensionInfo)) return false;
        if (formerKey == null) {
            if (other.formerKey != null) return false;
        } else if (!formerKey.equals(other.formerKey)) return false;
        if (key == null) {
            if (other.key != null) return false;
        } else if (!key.equals(other.key)) return false;
        return true;
    }
}
