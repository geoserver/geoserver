/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

public class Point implements Cloneable {

    public double x;
    public double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point add(Point point) {
        return this.clone()._add(point);
    }

    private Point _add(Point point) {
        this.x += point.x;
        this.y += point.y;
        return this;
    }

    public Point subtract(Point point) {
        return this.clone()._subtract(point);
    }

    private Point _subtract(Point point) {
        this.x -= point.x;
        this.y -= point.y;
        return this;
    }

    private Point _divideBy(double num) {
        this.x /= num;
        this.y /= num;
        return this;
    }

    public Point divideBy(double num) {
        return this.clone()._divideBy(num);
    }

    public Point multiplyBy(double num) {
        return this.clone()._multiplyBy(num);
    }

    private Point _multiplyBy(double num) {
        this.x *= num;
        this.y *= num;
        return this;
    }

    protected Point clone() {
        return new Point(this.x, this.y);
    }

    public Point floor() {
        return this.clone()._floor();
    }

    private Point _floor() {
        this.x = Math.floor(this.x);
        this.y = Math.floor(this.y);
        return this;
    }

    public Point ceil() {
        return this.clone()._ceil();
    }

    private Point _ceil() {
        this.x = Math.ceil(this.x);
        this.y = Math.ceil(this.y);
        return this;
    }

    public Point round() {
        return this.clone()._round();
    }

    private Point _round() {
        this.x = Math.round(this.x);
        this.y = Math.round(this.y);
        return this;
    }

    public double distanceTo(Point other) {
        double dx = Math.abs(other.x - this.x);
        double dy = Math.abs(other.y - this.y);
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }
}
