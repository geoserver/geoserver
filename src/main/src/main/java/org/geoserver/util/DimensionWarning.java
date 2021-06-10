/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.ResourceInfo;

/**
 * Collects all details of WMS dimension warning, can be translated to the expected header message
 */
public class DimensionWarning {

    static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    public enum WarningType {
        Default,
        Nearest,
        FailedNearest
    }

    String layerName;
    String dimensionName;
    Object value;
    String unit;
    WarningType warningType;

    public static DimensionWarning nearest(ResourceInfo r, String dimensionName, Object value) {
        return new DimensionWarning(r, dimensionName, value, WarningType.Nearest);
    }

    public static DimensionWarning defaultValue(
            ResourceInfo r, String dimensionName, Object value) {
        return new DimensionWarning(r, dimensionName, value, WarningType.Default);
    }

    public static DimensionWarning notFound(ResourceInfo r, String dimensionName) {
        return new DimensionWarning(r, dimensionName, null, WarningType.FailedNearest);
    }

    protected DimensionWarning(
            ResourceInfo resource, String dimensionName, Object value, WarningType warningType) {
        this.layerName = resource.prefixedName();
        this.dimensionName = dimensionName;
        this.value = value;
        DimensionInfo di = resource.getMetadata().get(getKey(dimensionName), DimensionInfo.class);
        this.unit = di.getUnits();
        this.warningType = warningType;
    }

    protected DimensionWarning(DimensionWarning other) {
        this.layerName = other.layerName;
        this.dimensionName = other.dimensionName;
        this.value = other.value;
        this.unit = other.unit;
        this.warningType = other.warningType;
    }

    private String getKey(String dimensionName) {
        if (ResourceInfo.TIME.equals(dimensionName) || ResourceInfo.ELEVATION.equals(dimensionName))
            return dimensionName;
        return ResourceInfo.CUSTOM_DIMENSION_PREFIX + dimensionName;
    }

    public String getHeader() {
        if (warningType == WarningType.FailedNearest) {
            return "99 No nearest value found on " + layerName + ": " + dimensionName;
        } else {
            String type = (warningType == WarningType.Nearest) ? "Nearest value" : "Default value";
            String unitSpec = unit == null ? "" : unit;
            String valueSpec = formatValue(value);
            return "99 "
                    + type
                    + " used: "
                    + dimensionName
                    + "="
                    + valueSpec
                    + " "
                    + unitSpec
                    + " ("
                    + layerName
                    + ")";
        }
    }

    private String formatValue(Object value) {
        if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(UTC_TZ);
            return sdf.format(value);
        } else if (value == null) {
            return "-";
        } else {
            // numbers mostly?
            return value.toString();
        }
    }

    public WarningType getWarningType() {
        return warningType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DimensionWarning that = (DimensionWarning) o;
        return Objects.equals(layerName, that.layerName)
                && Objects.equals(dimensionName, that.dimensionName)
                && Objects.equals(value, that.value)
                && Objects.equals(unit, that.unit)
                && warningType == that.warningType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(layerName, dimensionName, value, unit, warningType);
    }
}
