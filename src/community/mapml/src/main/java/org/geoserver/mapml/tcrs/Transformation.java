/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

public class Transformation {
    private final double a;
    private final double b;
    private final double c;
    private final double d;

    public Transformation(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public Point transform(Point p, double scale) {
        if (Double.compare(scale, 0D) == 0) {
            scale = 1.0D;
        }
        return new Point(scale * (this.a * p.x + this.b), scale * (this.c * p.y + this.d));
    }

    public Point untransform(Point p, double scale) {
        if (Double.compare(scale, 0D) == 0) {
            scale = 1.0D;
        }
        return new Point((p.x / scale - this.b) / this.a, (p.y / scale - this.d) / this.c);
    }
}
