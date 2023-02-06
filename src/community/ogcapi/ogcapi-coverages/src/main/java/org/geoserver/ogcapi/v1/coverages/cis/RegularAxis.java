/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages.cis;

/**
 * A Regular Axis is an axis where all direct coordinates are at a common distance from its
 * immediate neighbors. Upper and lower bounds are Objects as they can be both numbers and ISO
 * times.
 */
public class RegularAxis extends Axis {

    private final double resolution;
    private final Object lowerBound;
    private final Object upperBound;
    private final String uomLabel;

    public RegularAxis(
            String axisLabel,
            Object lowerBound,
            Object upperBound,
            double resolution,
            String uomLabel) {
        super("RegularAxisType", axisLabel);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.resolution = resolution;
        this.uomLabel = uomLabel;
    }

    public double getResolution() {
        return resolution;
    }

    public Object getLowerBound() {
        return lowerBound;
    }

    public Object getUpperBound() {
        return upperBound;
    }

    public String getUomLabel() {
        return uomLabel;
    }
}
