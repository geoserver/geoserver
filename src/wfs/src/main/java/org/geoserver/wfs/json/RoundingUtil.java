/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

/**
 * Utility class for rounding double values.
 *
 * @author Dean Povey
 */
public class RoundingUtil {
    // How to scale the double, indexed by the number of digits
    private static double[] SCALE = {
        1d, 10d, 100d, 1000d, 10000d, 100000d, 1000000d, 10000000d, 100000000d
    };

    /**
     * Round a value to the specified number of decimal places using the "Round Half Up" strategy.
     *
     * <p>Special cases:
     *
     * <ul>
     *   <li>NaN is returned as NaN.
     *   <li>
     *   <li>+/-Infinity are returned as +/-Infinity.
     * </ul>
     *
     * @param value The value to round
     * @param numDecimals The number of decimal places to round to.
     * @return The value rounded to the specified number of decimals
     */
    public static double round(double value, int numDecimals) {
        // Technically this code will handle -numDecimals by rounding digits to the left of the
        // decimal point, but that is
        // probably a use case that is not really needed in practice.

        double scale = (numDecimals < 8) ? SCALE[numDecimals] : Math.pow(10, numDecimals);

        // Prevent us exceeding the maximum precision.  We make sure the minimum spacing between
        // values is sufficient
        // for the given scale otherwise just return the value.
        if (Math.ulp(value) * scale > 1d) return value;

        return Math.floor(value * scale + 0.5) / scale;
    }
}
