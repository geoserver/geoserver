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

    @Override
    public boolean isEnabled() {
        return dimensionInfo.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        dimensionInfo.setEnabled(enabled);
    }

    @Override
    public String getAttribute() {
        return dimensionInfo.getAttribute();
    }

    @Override
    public void setAttribute(String attribute) {
        dimensionInfo.setAttribute(attribute);
    }

    @Override
    public String getEndAttribute() {
        return dimensionInfo.getEndAttribute();
    }

    @Override
    public void setEndAttribute(String attribute) {
        dimensionInfo.setEndAttribute(attribute);
    }

    @Override
    public DimensionPresentation getPresentation() {
        return dimensionInfo.getPresentation();
    }

    @Override
    public void setPresentation(DimensionPresentation presentation) {
        dimensionInfo.setPresentation(presentation);
    }

    @Override
    public BigDecimal getResolution() {
        return dimensionInfo.getResolution();
    }

    @Override
    public void setResolution(BigDecimal resolution) {
        dimensionInfo.setResolution(resolution);
    }

    @Override
    public String getUnits() {
        return dimensionInfo.getUnits();
    }

    @Override
    public void setUnits(String units) {
        dimensionInfo.setUnits(units);
    }

    @Override
    public String getUnitSymbol() {
        return dimensionInfo.getUnitSymbol();
    }

    @Override
    public void setUnitSymbol(String unitSymbol) {
        dimensionInfo.setUnitSymbol(unitSymbol);
    }

    @Override
    public DimensionDefaultValueSetting getDefaultValue() {
        return dimensionInfo.getDefaultValue();
    }

    @Override
    public void setDefaultValue(DimensionDefaultValueSetting defaultValue) {
        dimensionInfo.setDefaultValue(defaultValue);
    }

    @Override
    public boolean isNearestMatchEnabled() {
        return dimensionInfo.isNearestMatchEnabled();
    }

    @Override
    public void setNearestMatchEnabled(boolean nearestMatch) {
        dimensionInfo.setNearestMatchEnabled(nearestMatch);
    }

    @Override
    public String getAcceptableInterval() {
        return dimensionInfo.getAcceptableInterval();
    }

    @Override
    public void setAcceptableInterval(String acceptableInterval) {
        dimensionInfo.setAcceptableInterval(acceptableInterval);
    }

    @Override
    public String getStartValue() {
        return dimensionInfo.getStartValue();
    }

    @Override
    public void setStartValue(String startValue) {
        dimensionInfo.setStartValue(startValue);
    }

    @Override
    public String getEndValue() {
        return dimensionInfo.getEndValue();
    }

    @Override
    public void setEndValue(String endValue) {
        dimensionInfo.setEndValue(endValue);
    }

    @Override
    public NearestFailBehavior getNearestFailBehavior() {
        return dimensionInfo.getNearestFailBehavior();
    }

    @Override
    public void setNearestFailBehavior(NearestFailBehavior matchBehavior) {
        dimensionInfo.setNearestFailBehavior(matchBehavior);
    }

    @Override
    public boolean isRawNearestMatchEnabled() {
        // raw nearest match isn't implemented on vectors, yet.
        return false;
    }

    @Override
    public void setRawNearestMatchEnabled(boolean rawNearestMatch) {
        // raw nearest match isn't implemented on vectors, yet.
        dimensionInfo.setRawNearestMatchEnabled(false);
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
