package org.geoserver.bxml.filter_1_1.spatial;

/**
 * The Class Distance.
 * 
 * @author cfarina
 */
public class Distance {

    /** The value. */
    private double value;

    /** The units. */
    private String units;

    /**
     * Instantiates a new distance.
     * 
     * @param value
     *            the value
     * @param units
     *            the units
     */
    public Distance(double value, String units) {
        super();
        this.value = value;
        this.units = units;
    }

    /**
     * Gets the value.
     * 
     * @return the value
     */
    public double getValue() {
        return value;
    }

    /**
     * Sets the value.
     * 
     * @param value
     *            the new value
     */
    public void setValue(double value) {
        this.value = value;
    }

    /**
     * Gets the units.
     * 
     * @return the units
     */
    public String getUnits() {
        return units;
    }

    /**
     * Sets the units.
     * 
     * @param units
     *            the new units
     */
    public void setUnits(String units) {
        this.units = units;
    }

}
