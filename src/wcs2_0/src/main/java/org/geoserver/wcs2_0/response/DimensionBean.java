/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

/**
 * A simple Bean storing Dimension information such as name, unit, symbol, type and dataType.
 *
 * @author Daniele Romagnoli, GeoSolutions
 */
public class DimensionBean {

    /** Time and elevation have special management. */
    public enum DimensionType {
        TIME,
        ELEVATION,
        CUSTOM
    }

    private String name;

    private String unit;

    private String symbol;

    private String datatype;

    private DimensionType dimensionType;

    private boolean isRange;

    public DimensionBean(
            final String name,
            final String unit,
            final String symbol,
            final String datatype,
            final DimensionType dimensionType,
            final boolean isRange) {
        this.name = name;
        this.unit = unit;
        this.symbol = symbol;
        this.datatype = datatype;
        this.dimensionType = dimensionType;
        this.isRange = isRange;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public DimensionType getDimensionType() {
        return dimensionType;
    }

    public void setDimensionType(DimensionType dimensionType) {
        this.dimensionType = dimensionType;
    }

    public boolean isRange() {
        return isRange;
    }

    public void setRange(boolean isRange) {
        this.isRange = isRange;
    }

    @Override
    public String toString() {
        return "DimensionBean [name="
                + name
                + ", unit="
                + unit
                + ", symbol="
                + symbol
                + ", datatype="
                + datatype
                + ", dimensionType="
                + dimensionType
                + ", isRange="
                + isRange
                + "]";
    }
}
