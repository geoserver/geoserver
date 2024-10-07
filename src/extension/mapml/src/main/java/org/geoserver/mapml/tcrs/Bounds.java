/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** @author prushforth */
public class Bounds {

    protected Point min, max;

    /**
     * @param a
     * @param b
     */
    public Bounds(Point a, Point b) {
        this.extend(a);
        this.extend(b);
    }

    /** @param bounds */
    public Bounds(String bounds) {
        List<Double> ord;
        try {
            ord = parse(bounds);
        } catch (Exception e) {
            String msg = "Error parsing bounds parameter: " + bounds + e.getMessage();
            throw new RuntimeException(msg, e);
        }
        Point a = new Point(ord.get(0), ord.get(1));
        Point b = new Point(ord.get(2), ord.get(3));
        this.extend(a);
        this.extend(b);
    }
    /**
     * Parse a string into a List of doubles in presumed order: west,south,east,north
     *
     * @param bounds comma-separated list of doubles in west,south,east,north order
     * @return a List in the order west,south,east,north
     */
    public static List<Double> parse(String bounds) {

        List<String> ordinates = commaSeparatedStringToStringArray(bounds);

        if (ordinates.size() != 4) {
            throw new RuntimeException("Number of ordinates in bounds must be 4.");
        }

        List<Double> ordDoubles = new ArrayList<>();
        ordDoubles.add(Double.valueOf(ordinates.get(0)));
        ordDoubles.add(Double.valueOf(ordinates.get(1)));
        ordDoubles.add(Double.valueOf(ordinates.get(2)));
        ordDoubles.add(Double.valueOf(ordinates.get(3)));

        return ordDoubles;
    }

    /**
     * @param aString a comma-separated string of strings to be split
     * @return a list of strings of what was found between the commas
     */
    private static List<String> commaSeparatedStringToStringArray(String aString) {
        String[] splittArray = null;
        if (aString != null && !aString.equalsIgnoreCase("")) {
            splittArray = aString.split(",");
        }
        if (splittArray == null || splittArray.length != 4) {
            throw new RuntimeException(
                    "Invalid number of bounds parameters: "
                            + (splittArray == null ? 0 : splittArray.length));
        }
        return Arrays.asList(splittArray);
    }

    /**
     * @param point
     * @return
     */
    final Bounds extend(Point point) {
        if (min == null && max == null) {
            min = point.clone();
            max = point.clone();
        } else {
            Point mx = new Point(0, 0);
            Point mn = new Point(0, 0);
            mn.x = this.min != null ? Math.min(point.x, this.min.x) : point.x;
            mx.x = this.max != null ? Math.max(point.x, this.max.x) : point.x;
            mn.y = this.min != null ? Math.min(point.y, this.min.y) : point.y;
            mx.y = this.max != null ? Math.max(point.y, this.max.y) : point.y;
            min = mn;
            max = mx;
        }
        return this;
    }

    /** @return a point at the centre of this (bounds) */
    public Point getCentre() {
        return new Point((this.min.x + this.max.x) / 2, (this.min.y + this.max.y) / 2);
    }

    /** @return the "min" of the bounds */
    public Point getMin() {
        return this.min;
    }

    /** @return the "max" point of the bounds */
    public Point getMax() {
        return this.max;
    }

    /** @return the width of the bounds along the x axis */
    public double getWidth() {
        return this.max.x - this.min.x;
    }

    /** @return the height of the bounds along the y axis */
    public double getHeight() {
        return this.max.y - this.min.y;
    }

    /**
     * @param other
     * @return boolean whether these bounds and the other bounds intersect
     */
    public boolean intersects(Bounds other) {

        boolean xIntersects = (other.max.x >= this.min.x) && (other.min.x <= this.max.x);
        boolean yIntersects = (other.max.y >= this.min.y) && (other.min.y <= this.max.y);
        return xIntersects && yIntersects;
    }

    /**
     * @param other
     * @return boolean whether these bounds contain the other bounds
     */
    public boolean contains(Bounds other) {
        return (other.min.x >= this.min.x && other.max.x <= this.max.x)
                && (other.min.y >= this.min.y && other.max.y <= this.max.y);
    }

    @Override
    public String toString() {
        return "Bounds{" + "min=" + min + ", max=" + max + '}';
    }
}
