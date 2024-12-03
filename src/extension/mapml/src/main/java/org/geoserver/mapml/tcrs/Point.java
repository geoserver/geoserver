/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

/** @author prushforth */
public class Point implements Cloneable {

    /** */
    public double x;

    /** */
    public double y;

    /**
     * @param x
     * @param y
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @param point
     * @return a new Point that adds the coordinates to these coordinates
     */
    public Point add(Point point) {
        return this.clone()._add(point);
    }
    /**
     * @param point
     * @return adds the x,y coordinates of point to this.x/y, returns this.
     */
    private Point _add(Point point) {
        this.x += point.x;
        this.y += point.y;
        return this;
    }

    /**
     * @param point
     * @return new Point representing the subtraction of point.x/y from this.x/y
     */
    public Point subtract(Point point) {
        return this.clone()._subtract(point);
    }
    /**
     * @param point
     * @return subtraction of the point.x/y from this.x/y
     */
    private Point _subtract(Point point) {
        this.x -= point.x;
        this.y -= point.y;
        return this;
    }
    /**
     * @param num
     * @return division of this.x/y by num
     */
    private Point _divideBy(double num) {
        this.x /= num;
        this.y /= num;
        return this;
    }

    /**
     * @param num
     * @return new Point result of division of this.x/y by num
     */
    public Point divideBy(double num) {
        return this.clone()._divideBy(num);
    }

    /**
     * @param num
     * @return new Point result of multiple of this.x/y by num
     */
    public Point multiplyBy(double num) {
        return this.clone()._multiplyBy(num);
    }
    /**
     * @param num
     * @return multiple of this.x/y by num
     */
    private Point _multiplyBy(double num) {
        this.x *= num;
        this.y *= num;
        return this;
    }
    /** @return a cloned copy of this Point */
    @Override
    protected Point clone() {
        return new Point(this.x, this.y);
    }

    /** @return a new Point representing the Math.floor(this.x/y) */
    public Point floor() {
        return this.clone()._floor();
    }
    /** @return the Math.floor(this.x/y) */
    private Point _floor() {
        this.x = Math.floor(this.x);
        this.y = Math.floor(this.y);
        return this;
    }

    /** @return a new Point representing the Math.ceil(this.x/y) */
    public Point ceil() {
        return this.clone()._ceil();
    }
    /** @return this Point's ceiling Math.ceil(this.x/y); */
    private Point _ceil() {
        this.x = Math.ceil(this.x);
        this.y = Math.ceil(this.y);
        return this;
    }

    /** @return a new Point representing Math.round(this.x/y) */
    public Point round() {
        return this.clone()._round();
    }
    /** @return this Point, rounded by Math.round */
    private Point _round() {
        this.x = Math.round(this.x);
        this.y = Math.round(this.y);
        return this;
    }

    /**
     * @param other
     * @return the distance from this Point to the other Point
     */
    public double distanceTo(Point other) {
        double dx = Math.abs(other.x - this.x);
        double dy = Math.abs(other.y - this.y);
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** @return x coordinate value */
    public double getX() {
        return this.x;
    }

    /** @return y coordinate value */
    public double getY() {
        return this.y;
    }

    @Override
    public String toString() {
        return "Point{" + "x=" + x + ", y=" + y + '}';
    }
}
