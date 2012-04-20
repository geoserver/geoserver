/* Copyright (c) 2001, 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

/**
 * Helpers to extract components from a color represented as an integer, and back, as well as
 * methods to "pack" and unpack colors via bit shifts
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
final class ColorUtils {

    /**
     * Returns the alpha component of the specified color
     * 
     * @param color
     */
    public static int alpha(int color) {
        return ((color >> 24) & 0xFF);
    }

    /**
     * Returns the red component of the specified color
     * 
     * @param color
     */
    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    /**
     * Returns the green component of the specified color
     * 
     * @param color
     */
    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    /**
     * Returns the blue component of the specified color
     * 
     * @param color
     */
    public static int blue(int color) {
        return color & 0xFF;
    }

    /**
     * Puts back the four color components into a integer representation
     */
    public static int color(int red, int green, int blue, int alpha) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8)
                | ((blue & 0xFF) << 0);
    }

    /**
     * Bit shifts a color component, loosing the less significant bits
     * 
     * @param component
     * @param shift
     * @return
     */
    public static int shift(int component, int shift) {
        return component >> shift;
    }

    /**
     * Undoes what shift did, with some heuristics to preserve full "black", full "white" and
     * lighter colors
     * 
     * @param component
     * @param shift
     * @return
     */
    public static int unshift(int component, int shift) {
        if (component == 0) {
            return 0;
        } else {
            int shiftedMax = 255 >> shift;
            if (component == shiftedMax) {
                return 255;
            } else {
                return (component * 255 + shiftedMax / 2) / shiftedMax;
            }
        }
    }

    /**
     * Compares two longs, to be used in comparators
     * 
     * @param l1
     * @param l2
     * @return
     */
    public static int compareLong(long l1, long l2) {
        long diff = l1 - l2;
        if (diff == 0) {
            return 0;
        } else if (diff > 0) {
            return 1;
        } else {
            return -1;
        }
    }

}
