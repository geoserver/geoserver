package org.geotools.data.graticule.gridsupport;
/**
 * This is taken from Incongruous' answer to <a
 * href="http://stackoverflow.com/questions/8506881/nice-label-algorithm-for-charts-with-minimum-ticks">...</a>
 * As such it is licensed under cc by-sa 3.0
 */
public class NiceScale {

    private double minPoint;
    private double maxPoint;
    private double maxTicks = 10;
    private double tickSpacing;
    private double range;
    private double niceMin;
    private double niceMax;

    /**
     * Instantiates a new instance of the NiceScale class.
     *
     * @param min the minimum data point on the axis
     * @param max the maximum data point on the axis
     */
    public NiceScale(double min, double max) {
        this.minPoint = min;
        this.maxPoint = max;
        calculate();
    }

    /**
     * Calculate and update values for tick spacing and nice minimum and maximum data points on the
     * axis.
     */
    private void calculate() {
        this.range = niceNum(maxPoint - minPoint, false);
        this.tickSpacing = niceNum(range / (maxTicks - 1), true);
        this.niceMin = Math.floor(minPoint / tickSpacing) * tickSpacing;
        this.niceMax = Math.ceil(maxPoint / tickSpacing) * tickSpacing;
    }

    /**
     * Returns a "nice" number approximately equal to range Rounds the number if round = true Takes
     * the ceiling if round = false.
     *
     * @param range the data range
     * @param round whether to round the result
     * @return a "nice" number to be used for the data range
     */
    private double niceNum(double range, boolean round) {
        double exponent;
        /** exponent of range */
        double fraction;
        /** fractional part of range */
        double niceFraction;
        /** nice, rounded fraction */
        exponent = Math.floor(Math.log10(range));
        fraction = range / Math.pow(10, exponent);

        if (round) {
            if (fraction < 1.5) niceFraction = 1;
            else if (fraction < 3) niceFraction = 2;
            else if (fraction < 7) niceFraction = 5;
            else niceFraction = 10;
        } else {
            if (fraction <= 1) niceFraction = 1;
            else if (fraction <= 2) niceFraction = 2;
            else if (fraction <= 5) niceFraction = 5;
            else niceFraction = 10;
        }

        return niceFraction * Math.pow(10, exponent);
    }

    /**
     * Sets the minimum and maximum data points for the axis.
     *
     * @param minPoint the minimum data point on the axis
     * @param maxPoint the maximum data point on the axis
     */
    public void setMinMaxPoints(double minPoint, double maxPoint) {
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        calculate();
    }

    /**
     * Sets maximum number of tick marks we're comfortable with
     *
     * @param maxTicks the maximum number of tick marks for the axis
     */
    public void setMaxTicks(double maxTicks) {
        this.maxTicks = maxTicks;
        calculate();
    }

    public double getTickSpacing() {
        return tickSpacing;
    }

    public double getNiceMin() {
        return niceMin;
    }

    public double getNiceMax() {
        return niceMax;
    }
}
