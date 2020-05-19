/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import java.io.Serializable;

/** Holds data for the attributes globe computed bounds. */
class GlobeBounds implements Serializable {

    private static final long serialVersionUID = 1L;

    private double width;
    private double height;
    private int radius;

    public GlobeBounds(double width, double height, int radius) {
        this.width = width;
        this.height = height;
        this.radius = radius;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    @Override
    public String toString() {
        return "GlobeBounds [width=" + width + ", height=" + height + ", radius=" + radius + "]";
    }
}
